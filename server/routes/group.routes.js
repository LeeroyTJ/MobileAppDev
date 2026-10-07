const express = require('express');
const router = express.Router();
const pool = require('../db');
const { authenticateToken } = require('../middleware/auth');
const { isPositiveInt } = require('../utils/validators');

// POST /api/v1/groups/:groupId/assign
// Body: { studentId }
// Assigns an unassigned student into a group, respecting capacity.
router.post('/:groupId/assign', authenticateToken, async (req, res) => {
    const { groupId } = req.params;
    const { studentId } = req.body;

    if (!isPositiveInt(groupId) || !isPositiveInt(studentId)) {
        return res.status(400).json({
            success: false,
            error: { code: 'VALIDATION_ERROR', message: 'Valid groupId and studentId are required' }
        });
    }

    const connection = await pool.getConnection();
    try {
        await connection.beginTransaction();

        // Lock the group row so concurrent requests for the last spot
        // serialize here instead of racing.
        const [groupRows] = await connection.query(
            'SELECT * FROM lab_groups WHERE group_id = ? FOR UPDATE',
            [groupId]
        );
        if (groupRows.length === 0) {
            await connection.rollback();
            return res.status(404).json({ success: false, error: { code: 'GROUP_NOT_FOUND', message: 'Group not found' } });
        }
        const group = groupRows[0];

        const [studentRows] = await connection.query(
            'SELECT * FROM students WHERE student_id = ? AND deleted_at IS NULL',
            [studentId]
        );
        if (studentRows.length === 0) {
            await connection.rollback();
            return res.status(404).json({ success: false, error: { code: 'STUDENT_NOT_FOUND', message: 'Student not found' } });
        }
        const student = studentRows[0];

        if (req.user.role !== 'LECTURER' && student.account_id !== req.user.id) {
            await connection.rollback();
            return res.status(403).json({ success: false, error: { code: 'FORBIDDEN', message: 'Access denied' } });
        }

        const [[{ count }]] = await connection.query(
            'SELECT COUNT(*) as count FROM students WHERE group_id = ? AND deleted_at IS NULL',
            [groupId]
        );

        if (count >= group.capacity) {
            await connection.rollback();
            return res.status(409).json({ success: false, error: { code: 'GROUP_FULL', message: 'This group is full' } });
        }

        await connection.query(
            'UPDATE students SET group_id = ? WHERE student_id = ?',
            [groupId, studentId]
        );

        await connection.commit();
        return res.status(200).json({
            success: true,
            data: { groupId: Number(groupId), studentId: Number(studentId), placesRemaining: group.capacity - count - 1 }
        });

    } catch (error) {
        await connection.rollback();
        console.error('Group assignment error:', error);
        return res.status(500).json({ success: false, error: { code: 'INTERNAL_ERROR', message: 'Server error during assignment' } });
    } finally {
        connection.release();
    }
});

// POST /api/v1/groups/:groupId/transfer
// Body: { studentId }
// Moves a student from their current group into :groupId.
router.post('/:groupId/transfer', authenticateToken, async (req, res) => {
    const { groupId } = req.params;
    const { studentId } = req.body;

    if (!isPositiveInt(groupId) || !isPositiveInt(studentId)) {
        return res.status(400).json({
            success: false,
            error: { code: 'VALIDATION_ERROR', message: 'Valid groupId and studentId are required' }
        });
    }

    const connection = await pool.getConnection();
    try {
        await connection.beginTransaction();

        const [groupRows] = await connection.query(
            'SELECT * FROM lab_groups WHERE group_id = ? FOR UPDATE',
            [groupId]
        );
        if (groupRows.length === 0) {
            await connection.rollback();
            return res.status(404).json({ success: false, error: { code: 'GROUP_NOT_FOUND', message: 'Group not found' } });
        }
        const group = groupRows[0];

        const [studentRows] = await connection.query(
            'SELECT * FROM students WHERE student_id = ? AND deleted_at IS NULL',
            [studentId]
        );
        if (studentRows.length === 0) {
            await connection.rollback();
            return res.status(404).json({ success: false, error: { code: 'STUDENT_NOT_FOUND', message: 'Student not found' } });
        }
        const student = studentRows[0];
        const previousGroupId = student.group_id;

        if (req.user.role !== 'LECTURER' && student.account_id !== req.user.id) {
            await connection.rollback();
            return res.status(403).json({ success: false, error: { code: 'FORBIDDEN', message: 'Access denied' } });
        }

        const [[{ count }]] = await connection.query(
            'SELECT COUNT(*) as count FROM students WHERE group_id = ? AND deleted_at IS NULL',
            [groupId]
        );

        if (count >= group.capacity) {
            await connection.rollback();
            return res.status(409).json({
                success: false,
                error: { code: 'GROUP_FULL', message: 'Target group is full — your previous group was retained' },
                data: { previousGroupId }
            });
        }

        await connection.query(
            'UPDATE students SET group_id = ? WHERE student_id = ?',
            [groupId, studentId]
        );

        await connection.commit();
        return res.status(200).json({
            success: true,
            data: { groupId: Number(groupId), studentId: Number(studentId), previousGroupId }
        });

    } catch (error) {
        await connection.rollback();
        console.error('Group transfer error:', error);
        return res.status(500).json({ success: false, error: { code: 'INTERNAL_ERROR', message: 'Server error during transfer' } });
    } finally {
        connection.release();
    }
});

// GET /api/v1/groups — list all groups with current occupancy counts
router.get('/', authenticateToken, async (req, res) => {
    try {
        const [rows] = await pool.query(
            `SELECT g.group_id, g.group_code, g.name, g.capacity,
                    COUNT(s.student_id) AS occupied
             FROM lab_groups g
             LEFT JOIN students s ON s.group_id = g.group_id AND s.deleted_at IS NULL
             GROUP BY g.group_id, g.group_code, g.name, g.capacity
             ORDER BY g.group_code`
        );
        return res.status(200).json({ success: true, data: rows });
    } catch (error) {
        console.error('Group list error:', error);
        return res.status(500).json({ success: false, error: { code: 'INTERNAL_ERROR', message: 'Server error' } });
    }
});

module.exports = router;