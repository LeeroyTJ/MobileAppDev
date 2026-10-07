const express = require('express');
const router = express.Router();
const pool = require('../db');
const jwt = require('jsonwebtoken');

const {
    isValidStudentNumber,
    isValidName
} = require('../utils/validators');

const JWT_SECRET = process.env.JWT_SECRET;

if (!JWT_SECRET) {
    throw new Error('JWT_SECRET is not set. Check your .env file.');
}

function authenticateToken(req, res, next) {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];

    if (!token) {
        return res.status(401).json({
            success: false,
            error: {
                code: 'UNAUTHORIZED',
                message: 'Token missing'
            }
        });
    }

    jwt.verify(token, JWT_SECRET, (err, user) => {
        if (err) {
            return res.status(403).json({
                success: false,
                error: {
                    code: 'FORBIDDEN',
                    message: 'Invalid token'
                }
            });
        }

        req.user = user;
        next();
    });
}

/**
 * POST /api/v1/sync
 * Idempotent batch transaction processing
 */
router.post('/', authenticateToken, async (req, res) => {
    const { operations } = req.body;
    const userId = req.user.id;

    if (!Array.isArray(operations)) {
        return res.status(400).json({
            success: false,
            error: {
                code: 'INVALID_REQUEST',
                message: 'Operations array is required'
            }
        });
    }

    const results = [];

    for (const op of operations) {
        const operationId = op.operationId;
        const type = op.type || op.operationType;
        const entity = op.entity || op.entityType;
        const payload = op.payload || {};
        const baseVersion = op.baseVersion !== undefined ? op.baseVersion : payload.baseVersion;

        if (!operationId || !type || !entity) {
            results.push({
                operationId: operationId || null,
                status: 'REJECTED',
                data: {
                    code: 'INVALID_OPERATION',
                    message: 'operationId, type and entity are required'
                }
            });
            continue;
        }

        const connection = await pool.getConnection();

        try {
            await connection.beginTransaction();

            // =========================================================
            // TASK 12.1 & 12.2: IDEMPOTENCY CHECK
            // Inspect incoming operationId UUID against sync_operations table.
            // If operation was previously processed, return cached payload.
            // =========================================================
            const [existing] = await connection.execute(
                `SELECT status, response_payload FROM sync_operations WHERE operation_id = ?`,
                [operationId]
            );

            if (existing.length > 0) {
                let cachedPayload = {};
                try {
                    cachedPayload = JSON.parse(existing[0].response_payload || '{}');
                } catch (parseError) {
                    cachedPayload = {};
                }

                results.push({
                    operationId,
                    status: existing[0].status,
                    data: cachedPayload,
                    ...cachedPayload
                });

                await connection.commit();
                continue;
            }

            let responsePayload = {};
            let opStatus = 'APPLIED';

            // =========================================================
            // STUDENT OPERATIONS
            // =========================================================
            if (entity === 'STUDENT') {

                // -----------------------------------------------------
                // CREATE STUDENT
                // -----------------------------------------------------
                if (type === 'CREATE_STUDENT') {
                    const rawStudentNumber = payload?.studentNumber;
                    const rawStudentName = payload?.name || payload?.studentName;

                    // TASK 12.3: Input Sanitization & Regex Validation
                    const studentNumber = typeof rawStudentNumber === 'string'
                        ? rawStudentNumber.trim()
                        : rawStudentNumber;

                    const studentName = typeof rawStudentName === 'string'
                        ? rawStudentName.trim()
                        : rawStudentName;

                    if (!isValidStudentNumber(studentNumber)) {
                        opStatus = 'REJECTED';
                        responsePayload = {
                            code: 'INVALID_STUDENT_NUMBER',
                            message: 'Student number must be exactly 9 digits'
                        };
                    } else if (!isValidName(studentName)) {
                        opStatus = 'REJECTED';
                        responsePayload = {
                            code: 'INVALID_STUDENT_NAME',
                            message: 'Student name must be between 2 and 100 characters'
                        };
                    } else {
                        const [result] = await connection.execute(
                            `INSERT INTO students (
                                account_id,
                                programme_id,
                                student_number,
                                student_name,
                                version
                            ) VALUES (?, ?, ?, ?, 1)`,
                            [
                                userId,
                                payload.programmeId || 1,
                                studentNumber,
                                studentName
                            ]
                        );

                        responsePayload = {
                            id: result.insertId,
                            entityId: result.insertId,
                            version: 1
                        };
                    }
                }

                // -----------------------------------------------------
                // UPDATE STUDENT
                // -----------------------------------------------------
                else if (type === 'UPDATE_STUDENT') {
                    const studentId = payload?.id || op.entityId;
                    const rawStudentName = payload?.name || payload?.studentName;

                    // TASK 12.3: Input Sanitization & Regex Validation
                    const studentName = typeof rawStudentName === 'string'
                        ? rawStudentName.trim()
                        : rawStudentName;

                    if (!studentId) {
                        opStatus = 'REJECTED';
                        responsePayload = {
                            code: 'INVALID_OPERATION',
                            message: 'Student ID is required for UPDATE_STUDENT'
                        };
                    } else if (!isValidName(studentName)) {
                        opStatus = 'REJECTED';
                        responsePayload = {
                            code: 'INVALID_STUDENT_NAME',
                            message: 'Student name must be between 2 and 100 characters'
                        };
                    } else {
                        const [current] = await connection.execute(
                            `SELECT version FROM students WHERE student_id = ? AND deleted_at IS NULL`,
                            [studentId]
                        );

                        if (current.length === 0) {
                            opStatus = 'NOT_FOUND';
                            responsePayload = {
                                code: 'STUDENT_NOT_FOUND',
                                message: 'Student not found'
                            };
                        } else if (baseVersion !== undefined && baseVersion !== null && current[0].version !== baseVersion) {
                            opStatus = 'CONFLICT';
                            responsePayload = {
                                serverVersion: current[0].version
                            };
                        } else {
                            await connection.execute(
                                `UPDATE students SET student_name = ?, version = version + 1 WHERE student_id = ? AND version = ?`,
                                [
                                    studentName,
                                    studentId,
                                    baseVersion
                                ]
                            );

                            responsePayload = {
                                id: studentId,
                                entityId: studentId,
                                version: baseVersion + 1
                            };
                        }
                    }
                }

                // -----------------------------------------------------
                // DELETE STUDENT
                // -----------------------------------------------------
                else if (type === 'DELETE_STUDENT') {
                    const studentId = payload?.id || op.entityId;

                    if (!studentId) {
                        opStatus = 'REJECTED';
                        responsePayload = {
                            code: 'INVALID_OPERATION',
                            message: 'Student ID is required for DELETE_STUDENT'
                        };
                    } else {
                        const [current] = await connection.execute(
                            `SELECT version FROM students WHERE student_id = ? AND deleted_at IS NULL`,
                            [studentId]
                        );

                        if (current.length === 0) {
                            opStatus = 'NOT_FOUND';
                            responsePayload = {
                                code: 'STUDENT_NOT_FOUND',
                                message: 'Student not found'
                            };
                        } else if (baseVersion !== undefined && baseVersion !== null && current[0].version !== baseVersion) {
                            opStatus = 'CONFLICT';
                            responsePayload = {
                                serverVersion: current[0].version
                            };
                        } else {
                            await connection.execute(
                                `UPDATE students SET deleted_at = NOW(), group_id = NULL WHERE student_id = ?`,
                                [studentId]
                            );

                            responsePayload = {
                                id: studentId,
                                entityId: studentId,
                                version: current[0].version + 1
                            };
                        }
                    }
                }

                // -----------------------------------------------------
                // UNKNOWN STUDENT OPERATION
                // -----------------------------------------------------
                else {
                    opStatus = 'REJECTED';
                    responsePayload = {
                        code: 'UNKNOWN_OPERATION',
                        message: `Unsupported student operation: ${type}`
                    };
                }
            }

            // =========================================================
            // UNKNOWN ENTITY
            // =========================================================
            else {
                opStatus = 'REJECTED';
                responsePayload = {
                    code: 'UNKNOWN_ENTITY',
                    message: `Unsupported entity: ${entity}`
                };
            }

            // =========================================================
            // TASK 12.1: STORE OPERATION RECEIPT & RESULT
            // =========================================================
            await connection.execute(
                `INSERT INTO sync_operations (
                    operation_id,
                    account_id,
                    operation_type,
                    entity_type,
                    status,
                    response_payload
                ) VALUES (?, ?, ?, ?, ?, ?)`,
                [
                    operationId,
                    userId,
                    type,
                    entity,
                    opStatus,
                    JSON.stringify(responsePayload)
                ]
            );

            await connection.commit();

            results.push({
                operationId,
                status: opStatus,
                data: responsePayload,
                ...responsePayload
            });

        } catch (error) {
            await connection.rollback();
            console.error(`Error processing operation ${operationId}:`, error);

            results.push({
                operationId,
                status: 'ERROR',
                message: error.message
            });
        } finally {
            connection.release();
        }
    }

    return res.json({
        success: true,
        data: {
            results
        },
        results
    });
});

module.exports = router;
