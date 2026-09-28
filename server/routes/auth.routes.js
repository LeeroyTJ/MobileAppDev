const express = require('express');
const router = express.Router();
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const pool = require('../db');

const {
    isValidStudentNumber,
    isValidName,
    isValidPassword,
    isValidClaimCode,
    isValidProgrammeId
} = require('../utils/validators');

const JWT_SECRET = process.env.JWT_SECRET;
if (!JWT_SECRET) {
    throw new Error('JWT_SECRET is not set. Check your .env file.');
}

/**
 * POST /api/v1/auth/register
 * Public registration endpoint
 */
router.post('/register', async (req, res) => {
    const { studentNumber, name, password, claimCode } = req.body;

    if (!studentNumber || !name || !password) {
        return res.status(400).json({
            success: false,
            error: { code: 'VALIDATION_ERROR', message: 'Missing required fields' }
        });
    }

    if (!isValidStudentNumber(studentNumber)) {
        return res.status(400).json({
            success: false,
            error: { code: 'INVALID_STUDENT_NUMBER', message: 'Student number must be exactly 9 digits' }
        });
    }

    const trimmedName = name.trim();
    if (!isValidName(trimmedName)) {
        return res.status(400).json({
            success: false,
            error: { code: 'INVALID_NAME', message: 'Name must be between 2 and 100 characters' }
        });
    }

    if (!isValidPassword(password)) {
        return res.status(400).json({
            success: false,
            error: { code: 'INVALID_PASSWORD', message: 'Password must be at least 8 characters' }
        });
    }

    if (claimCode && !isValidClaimCode(claimCode)) {
        return res.status(400).json({
            success: false,
            error: { code: 'INVALID_CLAIM_CODE', message: 'Claim code format is invalid' }
        });
    }

    const connection = await pool.getConnection();
    try {
        await connection.beginTransaction();

        // Check for an existing (possibly lecturer pre-entered) student with this number
        const [existingStudents] = await connection.query(
            'SELECT * FROM students WHERE student_number = ? AND deleted_at IS NULL',
            [studentNumber]
        );

        let studentId, accountId;

        if (existingStudents.length > 0) {
            const existing = existingStudents[0];

            if (existing.account_id !== null) {
                // Already claimed by someone
                await connection.rollback();
                return res.status(409).json({
                    success: false,
                    error: { code: 'STUDENT_ALREADY_REGISTERED', message: 'This student number is already registered' }
                });
            }

            // Pre-entered but unclaimed — a claim code is required
            if (!claimCode) {
                await connection.rollback();
                return res.status(400).json({
                    success: false,
                    error: { code: 'CLAIM_CODE_REQUIRED', message: 'This student number is on file. Enter your claim code to confirm this is you.' }
                });
            }

            const codeHash = crypto.createHash('sha256').update(claimCode).digest('hex');
            const [claimRows] = await connection.query(
                'SELECT * FROM claim_codes WHERE code_hash = ? AND student_id = ? AND used_at IS NULL AND expires_at > NOW()',
                [codeHash, existing.student_id]
            );

            if (claimRows.length === 0) {
                await connection.rollback();
                return res.status(400).json({
                    success: false,
                    error: { code: 'INVALID_CLAIM_CODE', message: 'Invalid, already used, expired, or mismatched claim code' }
                });
            }

            const passwordHash = await bcrypt.hash(password, 10);
            const [accountResult] = await connection.query(
                'INSERT INTO accounts (username, password_hash, role, is_active) VALUES (?, ?, "STUDENT", TRUE)',
                [studentNumber, passwordHash]
            );
            accountId = accountResult.insertId;
            studentId = existing.student_id;

            // Link, don't create
            await connection.query(
                'UPDATE students SET account_id = ? WHERE student_id = ?',
                [accountId, studentId]
            );
            await connection.query(
                'UPDATE claim_codes SET used_at = NOW(), used_by_account_id = ? WHERE claim_code_id = ?',
                [accountId, claimRows[0].claim_code_id]
            );

        } else {

            if (!isValidProgrammeId(req.body.programmeId)) {
                await connection.rollback();
                return res.status(400).json({
                    success: false,
                    error: { code: 'INVALID_PROGRAMME_ID', message: 'Valid programmeId is required for new students' }
                });
            }

            // No pre-entered record — genuinely new student, no claim code needed
            const passwordHash = await bcrypt.hash(password, 10);
            const [accountResult] = await connection.query(
                'INSERT INTO accounts (username, password_hash, role, is_active) VALUES (?, ?, "STUDENT", TRUE)',
                [studentNumber, passwordHash]
            );
            accountId = accountResult.insertId;

            // NOTE: programme_id needs to come from the request body for a
            // brand-new registration, since there's no pre-entered record to
            // read it from. Confirm with your UI team that the registration
            // form sends programmeId.
            const [studentResult] = await connection.query(
                'INSERT INTO students (account_id, programme_id, student_number, student_name) VALUES (?, ?, ?, ?)',
                [accountId, req.body.programmeId, studentNumber, trimmedName]
            );
            studentId = studentResult.insertId;
        }

        await connection.commit();
        return res.status(201).json({ success: true, data: { accountId, studentId } });

    } catch (error) {
        await connection.rollback();
        console.error('Registration error:', error);
        return res.status(500).json({
            success: false,
            error: { code: 'INTERNAL_ERROR', message: 'Server error during registration' }
        });
    } finally {
        connection.release();
    }
});

module.exports = router;