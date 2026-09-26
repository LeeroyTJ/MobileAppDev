const express = require('express');
const router = express.Router();
const pool = require('../db');
const jwt = require('jsonwebtoken');
const {
    isValidName,
    isValidBaseVersion,
    isValidProgrammeId,
    isPositiveInt,
    isValidSearch
} = require('../utils/validators');

const JWT_SECRET = process.env.JWT_SECRET;
if (!JWT_SECRET) {
    throw new Error('JWT_SECRET is not set. Check your .env file.');
}

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

// GET /api/v1/students — lecturer roster with combined filters
router.get('/', authenticateToken, async (req, res) => {
    const { programme, group, search } = req.query;

    if (programme && !['CS', 'IT', 'DS'].includes(programme)) {
        return res.status(400).json({
            success: false,
            error: { code: 'INVALID_PROGRAMME', message: 'programme must be CS, IT, or DS' }
        });
    }

    if (group && group !== 'UNASSIGNED' && !isPositiveInt(group)) {
        return res.status(400).json({
            success: false,
            error: { code: 'INVALID_GROUP', message: 'group must be UNASSIGNED or a valid group id' }
        });
    }

    if (search && !isValidSearch(search)) {
        return res.status(400).json({
            success: false,
            error: { code: 'INVALID_SEARCH', message: 'search must be 100 characters or fewer' }
        });
    }

    const where = ['s.deleted_at IS NULL'];
    const params = [];

    if (programme) {
        where.push('p.code = ?');
        params.push(programme);
    }

    if (group === 'UNASSIGNED') {
        where.push('s.group_id IS NULL');
    } else if (group) {
        where.push('s.group_id = ?');
        params.push(group);
    }

    if (search) {
        where.push('s.student_name LIKE ?');
        params.push(`%${search}%`);
    }

    try {
        const [rows] = await pool.query(
            `SELECT s.student_id, s.student_number, s.student_name,
                    s.programme_id, s.group_id, s.version,
                    p.code AS programme_code, p.name AS programme_name,
                    g.group_code, g.name AS group_name
             FROM students s
             JOIN programmes p ON s.programme_id = p.programme_id
             LEFT JOIN lab_groups g ON s.group_id = g.group_id
             WHERE ${where.join(' AND ')}
             ORDER BY s.student_name`,
            params
        );

        return res.status(200).json({ success: true, data: rows });
    } catch (error) {
        console.error('Roster fetch error:', error);
        return res.status(500).json({
            success: false,
            error: { code: 'INTERNAL_ERROR', message: 'Server error' }
        });
    }
});

// GET /api/v1/students/me/profile
router.get('/me/profile', authenticateToken, async (req, res) => {
    const { programme, group, search } = req.query;

    if (programme && !['CS', 'IT', 'DS'].includes(programme)) {
        return res.status(400).json({
            success: false,
            error: { code: 'INVALID_PROGRAMME', message: 'programme must be CS, IT, or DS' }
        });
    }

    if (group && group !== 'UNASSIGNED' && !isPositiveInt(group)) {
        return res.status(400).json({
            success: false,
            error: { code: 'INVALID_GROUP', message: 'group must be UNASSIGNED or a valid group id' }
        });
    }

    if (search && !isValidSearch(search)) {
        return res.status(400).json({
            success: false,
            error: { code: 'INVALID_SEARCH', message: 'search must be 100 characters or fewer' }
        });
    }

    try {
        const [rows] = await pool.query(
            `SELECT s.*, p.code as programme_code, p.name as programme_name, g.group_code, g.name as group_name
             FROM students s
             JOIN programmes p ON s.programme_id = p.programme_id
             LEFT JOIN lab_groups g ON s.group_id = g.group_id
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
    const { studentName, baseVersion } = req.body;

    if (!isPositiveInt(req.params.id)) {
        return res.status(400).json({
            success: false,
            error: { code: 'INVALID_STUDENT_ID', message: 'Invalid student id' }
        });
    }

    if (studentName !== undefined && !isValidName(studentName)) {
        return res.status(400).json({
            success: false,
            error: { code: 'INVALID_NAME', message: 'Name must be between 2 and 100 characters' }
        });
    }

    if (baseVersion !== undefined && !isValidBaseVersion(baseVersion)) {
        return res.status(400).json({
            success: false,
            error: { code: 'INVALID_BASE_VERSION', message: 'baseVersion must be a positive integer' }
        });
    }

    if (req.user.role !== 'LECTURER') {
        delete req.body.programmeId;
    } else if (req.body.programmeId !== undefined && !isValidProgrammeId(req.body.programmeId)) {
        return res.status(400).json({
            success: false,
            error: { code: 'INVALID_PROGRAMME_ID', message: 'Invalid programmeId' }
        });
    }

    const connection = await pool.getConnection();
    try {
        await connection.beginTransaction();

        const [result] = await connection.query(
            `UPDATE students
             SET student_name = COALESCE(?, student_name),
                 version = version + 1
             WHERE student_id = ? AND deleted_at IS NULL`,
            [studentName, studentId]
        );

        if (result.affectedRows === 0) {
            await connection.rollback();
            return res.status(404).json({
                success: false,
                error: { code: 'STUDENT_NOT_FOUND', message: 'Student not found' }
            });
        }

        await connection.commit();
        return res.status(200).json({
            success: true,
            data: { updated: true, version: (baseVersion || 0) + 1 }
        });

    } catch (error) {
        await connection.rollback();
        console.error('Update error:', error);
        return res.status(500).json({
            success: false,
            error: { code: 'INTERNAL_ERROR', message: 'Server error during update' }
        });
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

        console.error('Delete error:', error);
        return res.status(500).json({ success: false, error: { code: 'INTERNAL_ERROR', message: 'Server error during deletion' } });
    } finally {
        connection.release();
    }
});

module.exports = router;