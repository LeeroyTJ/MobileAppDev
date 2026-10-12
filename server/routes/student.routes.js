const express = require('express');
const router = express.Router();
const pool = require('../db');
const crypto = require('crypto');
const { authenticateToken, verifyRole } = require('../middleware/auth');
const {
    isValidName,
    isValidBaseVersion,
    isValidProgrammeId,
    isPositiveInt,
    isValidSearch,
    isValidStudentNumber,
    isValidGroupId
} = require('../utils/validators');

/**
 * GET /api/v1/students — lecturer roster with combined filters, indexed pagination (LIMIT, OFFSET), and SQL LIKE search
 */
router.get('/', authenticateToken, verifyRole('LECTURER'), async (req, res) => {
    const { programme, group, search, limit, offset } = req.query;

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

    let limitVal = 50;
    let offsetVal = 0;
    if (limit !== undefined) {
        if (!isPositiveInt(limit) || Number(limit) > 200) {
            return res.status(400).json({
                success: false,
                error: { code: 'INVALID_LIMIT', message: 'limit must be a positive integer <= 200' }
            });
        }
        limitVal = Number(limit);
    }
    if (offset !== undefined) {
        const offsetNum = Number(offset);
        if (!Number.isInteger(offsetNum) || offsetNum < 0) {
            return res.status(400).json({
                success: false,
                error: { code: 'INVALID_OFFSET', message: 'offset must be a non-negative integer' }
            });
        }
        offsetVal = offsetNum;
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
        where.push('(s.student_name LIKE ? OR s.student_number LIKE ?)');
        params.push(`%${search}%`, `%${search}%`);
    }

    try {
        params.push(limitVal, offsetVal);
        const [rows] = await pool.query(
            `SELECT s.student_id, s.student_number, s.student_name,
                    s.programme_id, s.group_id, s.version,
                    p.code AS programme_code, p.name AS programme_name,
                    g.group_code, g.name AS group_name
             FROM students s
             JOIN programmes p ON s.programme_id = p.programme_id
             LEFT JOIN lab_groups g ON s.group_id = g.group_id
             WHERE ${where.join(' AND ')}
             ORDER BY s.student_name
             LIMIT ? OFFSET ?`,
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

/**
 * POST /api/v1/students — lecturer creates a student
 */
router.post('/', authenticateToken, verifyRole('LECTURER'), async (req, res) => {
    const { studentNumber, studentName, programmeId, groupId } = req.body;

    if (!isValidStudentNumber(studentNumber)) {
        return res.status(400).json({ success: false, error: { code: 'INVALID_STUDENT_NUMBER', message: 'Student number must be exactly 9 digits' } });
    }
    if (!isValidName(studentName)) {
        return res.status(400).json({ success: false, error: { code: 'INVALID_NAME', message: 'Name must be between 2 and 100 characters' } });
    }
    if (!isValidProgrammeId(programmeId)) {
        return res.status(400).json({ success: false, error: { code: 'INVALID_PROGRAMME_ID', message: 'Valid programmeId is required' } });
    }
    if (groupId !== undefined && groupId !== null && !isValidGroupId(groupId)) {
        return res.status(400).json({ success: false, error: { code: 'INVALID_GROUP', message: 'Invalid groupId' } });
    }

    let connection;
    try {
        connection = await pool.getConnection();
        await connection.beginTransaction();

        if (groupId) {
            const [groupRows] = await connection.query(
                'SELECT * FROM lab_groups WHERE group_id = ? FOR UPDATE', [groupId]
            );
            if (groupRows.length === 0) {
                await connection.rollback();
                return res.status(404).json({ success: false, error: { code: 'GROUP_NOT_FOUND', message: 'Group not found' } });
            }
            const [[{ count }]] = await connection.query(
                'SELECT COUNT(*) AS count FROM students WHERE group_id = ? AND deleted_at IS NULL', [groupId]
            );
            if (count >= groupRows[0].capacity) {
                await connection.rollback();
                return res.status(409).json({ success: false, error: { code: 'GROUP_FULL', message: 'This group is full' } });
            }
        }

        const [result] = await connection.query(
            `INSERT INTO students (programme_id, group_id, student_number, student_name)
             VALUES (?, ?, ?, ?)`,
            [programmeId, groupId || null, studentNumber.trim(), studentName.trim()]
        );

        await connection.commit();
        return res.status(201).json({ success: true, data: { studentId: result.insertId } });

    } catch (error) {
        if (connection) await connection.rollback();
        if (error.code === 'ER_DUP_ENTRY') {
            return res.status(409).json({ success: false, error: { code: 'DUPLICATE_STUDENT_NUMBER', message: 'A student with this student number already exists' } });
        }
        if (error.code === 'ER_NO_REFERENCED_ROW_2') {
            return res.status(400).json({ success: false, error: { code: 'INVALID_PROGRAMME_ID', message: 'Programme does not exist' } });
        }
        console.error('Create student error:', error);
        return res.status(500).json({ success: false, error: { code: 'INTERNAL_ERROR', message: 'Server error' } });
    } finally {
        if (connection) connection.release();
    }
});

/**
 * GET /api/v1/students/me/profile — student views own profile (strictly restricted to req.user.id)
 */
router.get('/me/profile', authenticateToken, async (req, res) => {
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

/**
 * GET /api/v1/students/:id — lecturer: any student; student: own record only
 */
router.get('/:id', authenticateToken, async (req, res) => {
    const studentId = req.params.id;

    if (!isPositiveInt(studentId)) {
        return res.status(400).json({
            success: false,
            error: { code: 'INVALID_STUDENT_ID', message: 'Invalid student id' }
        });
    }

    try {
        const [rows] = await pool.query(
            `SELECT s.student_id, s.account_id, s.student_number, s.student_name,
                    s.programme_id, s.group_id, s.version,
                    p.code AS programme_code, p.name AS programme_name,
                    g.group_code, g.name AS group_name
             FROM students s
             JOIN programmes p ON s.programme_id = p.programme_id
             LEFT JOIN lab_groups g ON s.group_id = g.group_id
             WHERE s.student_id = ? AND s.deleted_at IS NULL`,
            [studentId]
        );

        if (rows.length === 0) {
            return res.status(404).json({ success: false, error: { code: 'STUDENT_NOT_FOUND', message: 'Student not found' } });
        }

        const student = rows[0];
        if (req.user.role !== 'LECTURER' && student.account_id !== req.user.id) {
            return res.status(403).json({ success: false, error: { code: 'FORBIDDEN', message: 'Access denied' } });
        }

        return res.status(200).json({ success: true, data: student });
    } catch (error) {
        console.error('Student fetch error:', error);
        return res.status(500).json({ success: false, error: { code: 'INTERNAL_ERROR', message: 'Server error' } });
    }
});

/**
 * PATCH /api/v1/students/:id/student-number — request student number correction
 */
router.patch('/:id/student-number', authenticateToken, async (req, res) => {
    const studentId = req.params.id;
    const { newStudentNumber } = req.body;

    if (!isPositiveInt(studentId)) {
        return res.status(400).json({ success: false, error: { code: 'INVALID_STUDENT_ID', message: 'Invalid student id' } });
    }
    if (!isValidStudentNumber(newStudentNumber)) {
        return res.status(400).json({ success: false, error: { code: 'INVALID_STUDENT_NUMBER', message: 'Student number must be exactly 9 digits' } });
    }
    const newNumber = newStudentNumber.trim();

    try {
        const [rows] = await pool.query(
            'SELECT student_id, account_id, student_number FROM students WHERE student_id = ? AND deleted_at IS NULL',
            [studentId]
        );
        if (rows.length === 0) {
            return res.status(404).json({ success: false, error: { code: 'STUDENT_NOT_FOUND', message: 'Student not found' } });
        }
        const student = rows[0];

        if (req.user.role !== 'LECTURER' && student.account_id !== req.user.id) {
            return res.status(403).json({ success: false, error: { code: 'FORBIDDEN', message: 'Access denied' } });
        }
        if (student.student_number === newNumber) {
            return res.status(400).json({ success: false, error: { code: 'NO_CHANGE', message: 'That is already your student number' } });
        }

        const [taken] = await pool.query('SELECT student_id FROM students WHERE student_number = ?', [newNumber]);
        if (taken.length > 0) {
            return res.status(409).json({ success: false, error: { code: 'STUDENT_NUMBER_TAKEN', message: 'That student number is already in use' } });
        }

        const operationId = crypto.randomUUID();
        await pool.query(
            `INSERT INTO sync_operations
                (operation_id, account_id, operation_type, entity_type, entity_id, status, response_payload)
             VALUES (?, ?, 'STUDENT_NUMBER_CORRECTION', 'student', ?, 'PROCESSING', ?)`,
            [operationId, req.user.id, student.student_id,
             JSON.stringify({ oldStudentNumber: student.student_number, newStudentNumber: newNumber })]
        );

        return res.status(202).json({ success: true, data: { operationId, status: 'PROCESSING' } });
    } catch (error) {
        console.error('Student number request error:', error);
        return res.status(500).json({ success: false, error: { code: 'INTERNAL_ERROR', message: 'Server error' } });
    }
});

/**
 * PUT /api/v1/students/:id — update student with optimistic concurrency & strict ownership checks
 */
router.put('/:id', authenticateToken, async (req, res) => {
    const studentId = req.params.id;
    const { studentName, baseVersion } = req.body;

    if (!isPositiveInt(studentId)) {
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

    if (baseVersion === undefined || !isValidBaseVersion(baseVersion)) {
        return res.status(400).json({
            success: false,
            error: { code: 'VALIDATION_ERROR', message: 'Valid baseVersion is required' }
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

        const [rows] = await connection.query(
            'SELECT * FROM students WHERE student_id = ? AND deleted_at IS NULL',
            [studentId]
        );
        if (rows.length === 0) {
            await connection.rollback();
            return res.status(404).json({
                success: false,
                error: { code: 'STUDENT_NOT_FOUND', message: 'Student not found' }
            });
        }
        const student = rows[0];

        // Task 11.2: Strict ownership enforcement
        if (req.user.role !== 'LECTURER' && student.account_id !== req.user.id) {
            await connection.rollback();
            return res.status(403).json({
                success: false,
                error: { code: 'FORBIDDEN', message: 'Access denied: Cannot edit records belonging to other users' }
            });
        }

        if (student.version !== Number(baseVersion)) {
            await connection.rollback();
            return res.status(409).json({
                success: false,
                error: { code: 'VERSION_CONFLICT', message: 'Record was modified on the server' },
                data: { currentVersion: student.version, current: student }
            });
        }

        const [result] = await connection.query(
            `UPDATE students
             SET student_name = COALESCE(?, student_name),
                 version = version + 1
             WHERE student_id = ? AND version = ? AND deleted_at IS NULL`,
            [studentName, studentId, baseVersion]
        );

        if (result.affectedRows === 0) {
            await connection.rollback();
            return res.status(409).json({
                success: false,
                error: { code: 'VERSION_CONFLICT', message: 'Record was modified on the server' }
            });
        }

        await connection.commit();
        return res.status(200).json({
            success: true,
            data: { updated: true, version: Number(baseVersion) + 1 }
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

/**
 * DELETE /api/v1/students/:id — Lecturer soft delete
 */
router.delete('/:id', authenticateToken, verifyRole('LECTURER'), async (req, res) => {
    const studentId = req.params.id;

    if (!isPositiveInt(studentId)) {
        return res.status(400).json({
            success: false,
            error: { code: 'INVALID_STUDENT_ID', message: 'Invalid student id' }
        });
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
