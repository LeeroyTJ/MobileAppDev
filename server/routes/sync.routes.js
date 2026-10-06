const express = require('express');
const router = express.Router();
const pool = require('../db');
const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET;
if (!JWT_SECRET) {
    throw new Error('JWT_SECRET is not set. Check your .env file.');
}

function authenticateToken(req, res, next) {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];
    if (!token) {
        return res.status(401).json({ success: false, error: { code: 'UNAUTHORIZED', message: 'Token missing' } });
    }
    jwt.verify(token, JWT_SECRET, (err, user) => {
        if (err) {
            return res.status(403).json({ success: false, error: { code: 'FORBIDDEN', message: 'Invalid token' } });
        }
        req.user = user;
        next();
    });
}

router.post('/', authenticateToken, async (req, res) => {
    const { operations } = req.body;
    const userId = req.user.id; // Extracted from verified JWT token

    if (!Array.isArray(operations)) {
        return res.status(400).json({ error: 'Operations array is required' });
    }

    const results = [];

    for (const op of operations) {
        const { operationId, type, entity, payload, baseVersion } = op;

        const connection = await pool.getConnection();
        try {
            await connection.beginTransaction();

            // 1. Idempotency Check: Has this operation already been processed?
            const [existing] = await connection.execute(
                'SELECT status, response_payload FROM sync_operations WHERE operation_id = ?',
                [operationId]
            );

            if (existing.length > 0) {
                results.push({
                    operationId,
                    status: existing[0].status,
                    data: JSON.parse(existing[0].response_payload || '{}')
                });
                await connection.commit();
                connection.release();
                continue;
            }

            // 2. Process based on entity and type
            let responsePayload = {};
            let opStatus = 'APPLIED';

            if (entity === 'STUDENT') {
                if (type === 'CREATE_STUDENT') {
                    const [result] = await connection.execute(
                        'INSERT INTO students (account_id, programme_id, student_number, student_name, version) VALUES (?, ?, ?, ?, 1)',
                        [userId, payload.programmeId, payload.studentNumber, payload.name]
                    );
                    responsePayload = { id: result.insertId, version: 1 };
                } else if (type === 'UPDATE_STUDENT') {
                    const [current] = await connection.execute(
                        'SELECT version FROM students WHERE student_id = ? AND deleted_at IS NULL',
                        [payload.id]
                    );

                    if (current.length === 0) {
                        opStatus = 'NOT_FOUND';
                    } else if (current[0].version !== baseVersion) {
                        opStatus = 'CONFLICT';
                        responsePayload = { serverVersion: current[0].version };
                    } else {
                        await connection.execute(
                            'UPDATE students SET student_name = ?, version = version + 1 WHERE student_id = ? AND version = ?',
                            [payload.name, payload.id, baseVersion]
                        );
                        responsePayload = { id: payload.id, version: baseVersion + 1 };
                    }
                }
            }

            // 3. Record operation in idempotency table
            await connection.execute(
                'INSERT INTO sync_operations (operation_id, account_id, operation_type, entity_type, status, response_payload) VALUES (?, ?, ?, ?, ?, ?)',
                [operationId, userId, type, entity, opStatus, JSON.stringify(responsePayload)]
            );

            await connection.commit();
            results.push({ operationId, status: opStatus, data: responsePayload });
        } catch (error) {
            await connection.rollback();
            console.error(`Error processing operation ${operationId}:`, error);
            results.push({ operationId, status: 'ERROR', message: error.message });
        } finally {
            connection.release();
        }
    }

    return res.json({ results });
});

module.exports = router;