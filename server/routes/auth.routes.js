const express = require('express');
const router = express.Router();
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const pool = require('../db');

const JWT_SECRET = process.env.JWT_SECRET || 'cohorthub_secret_key_change_me';

/**
 * POST /api/v1/auth/register
 * Public registration endpoint
 */
router.post('/register', async (req, res) => {
    const { username, name, password, claimCode } = req.body;

    if (!username || !name || !password || !claimCode) {
        return res.status(400).json({
            success: false,
            error: { code: 'VALIDATION_ERROR', message: 'Missing required fields' }
        });
    }

    const connection = await pool.getConnection();
    try {
        await connection.beginTransaction();

        const codeHash = crypto.createHash('sha256').update(claimCode).digest('hex');
        const [claimRows] = await connection.query(
            'SELECT * FROM claim_codes WHERE code_hash = ? AND used_at IS NULL AND expires_at > NOW()',
            [codeHash]
        );

        if (claimRows.length === 0) {
            await connection.rollback();
            return res.status(400).json({
                success: false,
                error: { code: 'INVALID_CLAIM_CODE', message: 'Invalid, already used, or expired claim code' }
            });
        }

        const claim = claimRows[0];

        const [existingAccounts] = await connection.query('SELECT account_id FROM accounts WHERE username = ?', [username]);
        if (existingAccounts.length > 0) {
            await connection.rollback();
            return res.status(409).json({
                success: false,
                error: { code: 'STUDENT_ALREADY_REGISTERED', message: 'Username or student number already registered' }
            });
        }

        const passwordHash = await bcrypt.hash(password, 10);
        const [accountResult] = await connection.query(
            'INSERT INTO accounts (username, password_hash, role, is_active) VALUES (?, ?, "STUDENT", TRUE)',
            [username, passwordHash]
        );
        const accountId = accountResult.insertId;

        const nameParts = name.trim().split(' ');
        const firstName = nameParts[0];
        const lastName = nameParts.slice(1).join(' ') || '';

        const [studentResult] = await connection.query(
            `INSERT INTO students (account_id, programme_id, student_number, first_name, last_name) VALUES (?, ?, ?, ?, ?)`,
            [accountId, claim.programme_id, username, firstName, lastName]
        );
        const studentId = studentResult.insertId;

        await connection.query(
            'UPDATE claim_codes SET used_at = NOW(), used_by_account_id = ? WHERE claim_code_id = ?',
            [accountId, claim.claim_code_id]
        );

        await connection.commit();
        return res.status(201).json({
            success: true,
            data: { accountId, studentId }
        });

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

/**
 * POST /api/v1/auth/login
 * Public login endpoint
 */
router.post('/login', async (req, res) => {
    const { username, password } = req.body;

    if (!username || !password) {
        return res.status(400).json({
            success: false,
            error: { code: 'VALIDATION_ERROR', message: 'Missing username or password' }
        });
    }

    try {
        const [rows] = await pool.query('SELECT * FROM accounts WHERE username = ?', [username]);
        if (rows.length === 0) {
            return res.status(401).json({
                success: false,
                error: { code: 'INVALID_CREDENTIALS', message: 'Invalid credentials' }
            });
        }

        const account = rows[0];
        if (!account.is_active) {
            return res.status(401).json({
                success: false,
                error: { code: 'ACCOUNT_DISABLED', message: 'Account is disabled' }
            });
        }

        const isMatch = await bcrypt.compare(password, account.password_hash);
        if (!isMatch) {
            return res.status(401).json({
                success: false,
                error: { code: 'INVALID_CREDENTIALS', message: 'Invalid credentials' }
            });
        }

        const token = jwt.sign({ id: account.account_id, role: account.role }, JWT_SECRET, { expiresIn: '7d' });
        return res.status(200).json({
            success: true,
            data: { accessToken: token, role: account.role, accountId: account.account_id }
        });

    } catch (error) {
        console.error('Login error:', error);
        return res.status(500).json({
            success: false,
            error: { code: 'INTERNAL_ERROR', message: 'Server error during login' }
        });
    }
});

module.exports = router;