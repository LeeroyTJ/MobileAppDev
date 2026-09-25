const express = require('express');
const router = express.Router();
const pool = require('../db');
const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'cohorthub_secret_key_change_me';

function authenticateToken(req, res, next) {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];
    if (!token) return res.status(401).json({ success: false, error: { code: 'UNAUTHORIZED', message: 'Token missing' } });

    jwt.verify(token, JWT_SECRET, (err, user) => {
        if (err) return res.status(403).json({ success: false, error: { code: 'FORBIDDEN', message: 'Invalid token' } });
        req.user = user;
        next();
    });
}

function requireLecturer(req, res, next) {
    if (req.user.role !== 'LECTURER') {
        return res.status(403).json({ success: false, error: { code: 'FORBIDDEN', message: 'Lecturer access required' } });
    }
    next();
}

// GET /api/v1/students/me/profile
router.get('/me/profile', authenticateToken, async (req, res) => {
    try {
        const [rows] = await pool.query(
            `SELECT s.*, p.code as programme_code, p.name as programme_name, g.group_code, g.name as group_name
             FROM students s
             JOIN programmes p ON s.programme_id = p.programme_id
             LEFT JOIN \`groups\` g ON s.group_id = g.group_id
             WHERE s.account_id = ? AND s.deleted_at IS NULL`,
            [req.user.id]
        );

        if (rows.length === 0) {
            return res.status(404).json({ success: false, error: { code: 'STUDENT_NOT_FOUND', message: 'Profile not found' } });
        }

        return res.status(200).json({ success: true, data: rows[0] });
    } catch (error) {
        console.error('Profile fetch error:', error);
        return res.status(500).json({ success: false, error: { code: 'INTERNAL_ERROR', message: 'Server error' } });
    }
});

// PUT /api/v1/students/:id (Optimistic Concurrency)
router.put('/:id', authenticateToken, async (req, res) => {
    const studentId = req.params.id;
    const { firstName, lastName, email, phone, groupId, baseVersion } = req.body;

    if (baseVersion === undefined) {
        return res.status(400).json({ success: false, error: { code: 'VALIDATION_ERROR', message: 'Missing baseVersion' } });
    }

    const connection = await pool.getConnection();
    try {
        await connection.beginTransaction();

        const [rows] = await connection.query('SELECT * FROM students WHERE student_id = ? AND deleted_at IS NULL', [studentId]);
        if (rows.length === 0) {
            await connection.rollback();
            return res.status(404).json({ success: false, error: { code: 'STUDENT_NOT_FOUND', message: 'Student not found' } });
        }

        const student = rows[0];
        if (req.user.role !== 'LECTURER' && student.account_id !== req.user.id) {
            await connection.rollback();
            return res.status(403).json({ success: false, error: { code: 'FORBIDDEN', message: 'Access denied' } });
        }

        if (student.version !== baseVersion) {
            await connection.rollback();
            return res.status(409).json({
                success: false,
                error: { code: 'VERSION_CONFLICT', message: 'Record modified on server' },
                data: { currentVersion: student.version, current: student }
            });
        }

        await connection.query(
            `UPDATE students
             SET first_name = COALESCE(?, first_name),
                 last_name = COALESCE(?, last_name),
                 email = COALESCE(?, email),
                 phone = COALESCE(?, phone),
                 group_id = COALESCE(?, group_id),
                 version = version + 1
             WHERE student_id = ?`,
            [firstName, lastName, email, phone, groupId, studentId]
        );

        await connection.commit();
        return res.status(200).json({ success: true, data: { updated: true, version: baseVersion + 1 } });

    } catch (error) {
        await connection.rollback();
        console.error('Update error:', error);
        return res.status(500).json({ success: false, error: { code: 'INTERNAL_ERROR', message: 'Server error during update' } });
    } finally {
        connection.release();
    }
});

// DELETE /api/v1/students/:id (Lecturer Soft Delete)
router.delete('/:id', authenticateToken, requireLecturer, async (req, res) => {
    const studentId = req.params.id;

    const connection = await pool.getConnection();
    try {
        await connection.beginTransaction();

        const [rows] = await connection.query('SELECT * FROM students WHERE student_id = ? AND deleted_at IS NULL', [studentId]);
        if (rows.length === 0) {
            await connection.rollback();
            return res.status(404).json({ success: false, error: { code: 'STUDENT_NOT_FOUND', message: 'Student not found' } });
        }

        const student = rows[0];

        await connection.query('UPDATE students SET deleted_at = NOW(), group_id = NULL WHERE student_id = ?', [studentId]);

        if (student.account_id) {
            await connection.query('UPDATE accounts SET is_active = FALSE WHERE account_id = ?', [student.account_id]);
        }

        await connection.commit();
        return res.status(200).json({ success: true, data: { deleted: true } });

    } catch (error) {
        await connection.rollback();
        console.error('Delete error:', error);
        return res.status(500).json({ success: false, error: { code: 'INTERNAL_ERROR', message: 'Server error during deletion' } });
    } finally {
        connection.release();
    }
});

module.exports = router;