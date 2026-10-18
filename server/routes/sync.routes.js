const express = require('express');
const router = express.Router();
const pool = require('../db');
const { authenticateToken } = require('../middleware/auth');
const {
    isValidStudentNumber,
    isValidName
} = require('../utils/validators');

/**
 * POST /api/v1/sync
 * Idempotent batch transaction processing
 */
router.post('/', authenticateToken, async (req, res) => {
    const { operations } = req.body;
    const userId = req.user.id;
    const userRole = req.user.role;

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
                const rawPayload = existing[0].response_payload;
                if (typeof rawPayload === 'string') {
                    try {
                        cachedPayload = JSON.parse(rawPayload || '{}');
                    } catch (parseError) {
                        cachedPayload = {};
                    }
                } else if (typeof rawPayload === 'object' && rawPayload !== null) {
                    cachedPayload = rawPayload;
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
                        // Pre-check for duplicate student number
                        const [existingStudent] = await connection.execute(
                            `SELECT student_id FROM students WHERE student_number = ?`,
                            [studentNumber]
                        );

                        if (existingStudent.length > 0) {
                            opStatus = 'REJECTED';
                            responsePayload = {
                                code: 'DUPLICATE_STUDENT_NUMBER',
                                message: 'A student with this student number already exists'
                            };
                        } else {
                            // Lecturer-created student has account_id = NULL
                            const accountIdForStudent = userRole === 'LECTURER' ? null : userId;

                            const [result] = await connection.execute(
                                `INSERT INTO students (
                                    account_id,
                                    programme_id,
                                    student_number,
                                    student_name,
                                    version
                                ) VALUES (?, ?, ?, ?, 1)`,
                                [
                                    accountIdForStudent,
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
                }

                // -----------------------------------------------------
                // UPDATE STUDENT
                // -----------------------------------------------------
                else if (type === 'UPDATE_STUDENT') {
                    const studentId = payload?.id || op.entityId;
                    const rawStudentName = payload?.name || payload?.studentName;

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
                            `SELECT account_id, version FROM students WHERE student_id = ? AND deleted_at IS NULL`,
                            [studentId]
                        );

                        if (current.length === 0) {
                            opStatus = 'NOT_FOUND';
                            responsePayload = {
                                code: 'STUDENT_NOT_FOUND',
                                message: 'Student not found'
                            };
                        } else if (userRole !== 'LECTURER' && current[0].account_id !== userId) {
                            opStatus = 'REJECTED';
                            responsePayload = {
                                code: 'FORBIDDEN',
                                message: 'Access denied: Cannot edit records belonging to other users'
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

                    if (userRole !== 'LECTURER') {
                        opStatus = 'REJECTED';
                        responsePayload = {
                            code: 'FORBIDDEN',
                            message: 'Lecturer access required for deletion'
                        };
                    } else if (!studentId) {
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
                // REQUEST GROUP CHANGE
                // -----------------------------------------------------
                else if (type === 'REQUEST_GROUP_CHANGE') {
                    const studentId = payload?.studentId || op.entityId;
                    const requestedGroupCode = payload?.requestedGroup;

                    if (userRole !== 'STUDENT' && userRole !== 'LECTURER') {
                        opStatus = 'REJECTED';
                        responsePayload = {
                            code: 'FORBIDDEN',
                            message: 'Only students can request a group change'
                        };
                    } else if (!studentId || !requestedGroupCode) {
                        opStatus = 'REJECTED';
                        responsePayload = {
                            code: 'INVALID_OPERATION',
                            message: 'studentId and requestedGroup are required'
                        };
                    } else {
                        // Resolve the target group code (G01..G04) to its id.
                        const [targetGroupRows] = await connection.execute(
                            'SELECT group_id, group_code, capacity FROM lab_groups WHERE group_code = ?',
                            [requestedGroupCode]
                        );

                        if (targetGroupRows.length === 0) {
                            opStatus = 'REJECTED';
                            responsePayload = {
                                code: 'GROUP_NOT_FOUND',
                                message: `No group with code ${requestedGroupCode}`
                            };
                        } else {
                            const targetGroupId = targetGroupRows[0].group_id;

                            // Confirm the student exists and, if a student, that they own the record.
                            const [studentRows] = await connection.execute(
                                'SELECT student_id, account_id, group_id, version FROM students WHERE student_id = ? AND deleted_at IS NULL',
                                [studentId]
                            );

                            if (studentRows.length === 0) {
                                opStatus = 'NOT_FOUND';
                                responsePayload = {
                                    code: 'STUDENT_NOT_FOUND',
                                    message: 'Student not found'
                                };
                            } else if (userRole !== 'LECTURER' && studentRows[0].account_id !== userId) {
                                opStatus = 'REJECTED';
                                responsePayload = {
                                    code: 'FORBIDDEN',
                                    message: 'Access denied'
                                };
                            } else if (studentRows[0].group_id === targetGroupId) {
                                // Already in that group — log it as APPROVED (no-op) and return APPLIED.
                                await connection.execute(
                                    `INSERT INTO group_requests
                                        (student_id, current_group_id, requested_group_id, status, operation_id, resolved_at)
                                     VALUES (?, ?, ?, 'APPROVED', ?, NOW())`,
                                    [studentId, studentRows[0].group_id, targetGroupId, operationId]
                                );

                                opStatus = 'APPLIED';
                                responsePayload = {
                                    id: studentId,
                                    entityId: studentId,
                                    groupId: targetGroupId,
                                    groupCode: requestedGroupCode,
                                    version: studentRows[0].version,
                                    noop: true
                                };
                            } else {
                                // Lock the target group row and count current occupancy.
                                const [lockRows] = await connection.execute(
                                    'SELECT capacity FROM lab_groups WHERE group_id = ? FOR UPDATE',
                                    [targetGroupId]
                                );
                                const capacity = lockRows[0].capacity;

                                const [[{ count }]] = await connection.execute(
                                    'SELECT COUNT(*) AS count FROM students WHERE group_id = ? AND deleted_at IS NULL',
                                    [targetGroupId]
                                );

                                if (count >= capacity) {
                                    const reason = `${requestedGroupCode} is full (${count}/${capacity})`;

                                    // Log the rejection
                                    await connection.execute(
                                        `INSERT INTO group_requests
                                            (student_id, current_group_id, requested_group_id, status, rejection_reason, operation_id, resolved_at)
                                         VALUES (?, ?, ?, 'REJECTED', ?, ?, NOW())`,
                                        [studentId, studentRows[0].group_id, targetGroupId, reason, operationId]
                                    );

                                    opStatus = 'REJECTED';
                                    responsePayload = {
                                        code: 'GROUP_FULL',
                                        message: reason
                                    };
                                } else {
                                    await connection.execute(
                                        'UPDATE students SET group_id = ?, version = version + 1 WHERE student_id = ?',
                                        [targetGroupId, studentId]
                                    );

                                    // Log the approval
                                    await connection.execute(
                                        `INSERT INTO group_requests
                                            (student_id, current_group_id, requested_group_id, status, operation_id, resolved_at)
                                         VALUES (?, ?, ?, 'APPROVED', ?, NOW())`,
                                        [studentId, studentRows[0].group_id, targetGroupId, operationId]
                                    );

                                    opStatus = 'APPLIED';
                                    responsePayload = {
                                        id: studentId,
                                        entityId: studentId,
                                        groupId: targetGroupId,
                                        groupCode: requestedGroupCode,
                                        version: studentRows[0].version + 1
                                    };
                                }
                            }
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

            if (error.code === 'ER_DUP_ENTRY') {
                const dupPayload = {
                    code: 'DUPLICATE_STUDENT_NUMBER',
                    message: 'A student with this student number already exists'
                };
                try {
                    await pool.execute(
                        `INSERT INTO sync_operations (
                            operation_id, account_id, operation_type, entity_type, status, response_payload
                        ) VALUES (?, ?, ?, ?, 'REJECTED', ?)`,
                        [operationId, userId, type, entity, JSON.stringify(dupPayload)]
                    );
                } catch (ignored) {}

                results.push({
                    operationId,
                    status: 'REJECTED',
                    message: 'A student with this student number already exists',
                    data: dupPayload
                });
            } else {
                results.push({
                    operationId,
                    status: 'ERROR',
                    message: error.message
                });
            }
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

/**
 * GET /api/v1/sync/changes?since=...
 * Returns incremental delta changes updated since timestamp
 */
router.get('/changes', authenticateToken, async (req, res) => {
    const { since } = req.query;

    // Parse `since` once — used by both queries below.
    let sinceDate = null;
    if (since) {
        let dateVal;
        if (/^\d+$/.test(since)) {
            dateVal = new Date(Number(since));
        } else {
            dateVal = new Date(since);
        }
        if (!isNaN(dateVal.getTime())) {
            sinceDate = dateVal;
        }
    }

    try {
        // ---------- 1. Student changes ----------
        let studentQuery = `
            SELECT s.*, p.code AS programme_code, g.group_code
            FROM students s
            JOIN programmes p ON s.programme_id = p.programme_id
            LEFT JOIN lab_groups g ON s.group_id = g.group_id`;
        const studentParams = [];

        if (sinceDate) {
            studentQuery += ' WHERE s.updated_at >= ?';
            studentParams.push(sinceDate);
        }
        studentQuery += ' ORDER BY s.updated_at ASC';

        const [studentRows] = await pool.query(studentQuery, studentParams);

        // ---------- 2. Group-request resolutions ----------
        // Only resolved rows (APPROVED/REJECTED) are "changes" — PENDING hasn't happened.
        // A student sees only their own requests; a lecturer sees all.
        let requestQuery = `
            SELECT
                gr.request_id,
                gr.student_id,
                s.student_name,
                s.student_number,
                gr.current_group_id,
                cg.group_code AS current_group_code,
                gr.requested_group_id,
                rg.group_code AS requested_group_code,
                gr.status,
                gr.rejection_reason,
                gr.operation_id,
                gr.created_at,
                gr.resolved_at
            FROM group_requests gr
            JOIN students s ON gr.student_id = s.student_id
            LEFT JOIN lab_groups cg ON gr.current_group_id = cg.group_id
            JOIN lab_groups rg ON gr.requested_group_id = rg.group_id
            WHERE gr.status IN ('APPROVED', 'REJECTED')
              AND gr.resolved_at >= ?`;
        const requestParams = [];

        if (sinceDate) {
            requestParams.push(sinceDate);
        } else {
            // If no `since` given, return the last 30 days of resolutions.
            // Prevents the first sync from downloading years of history.
            requestParams.push(new Date(Date.now() - 30 * 24 * 60 * 60 * 1000));
        }

        // Scope: students see only their own requests; lecturers see all.
        if (req.user.role === 'STUDENT') {
            requestQuery += ' AND s.account_id = ?';
            requestParams.push(req.user.id);
        }

        requestQuery += ' ORDER BY gr.resolved_at ASC';

        const [requestRows] = await pool.query(requestQuery, requestParams);

        return res.status(200).json({
            success: true,
            data: {
                changes: studentRows,          // kept for backward compatibility
                students: studentRows,         // new explicit name (same data)
                group_requests: requestRows,   // new field
                serverTime: new Date().toISOString()
            }
        });
    } catch (error) {
        console.error('Sync changes error:', error);
        return res.status(500).json({
            success: false,
            error: { code: 'INTERNAL_ERROR', message: 'Server error' }
        });
    }
});

module.exports = router;
