// Creates a LECTURER account with a real bcrypt hash.
// Save as: server/scripts/create-lecturer.js
// Run from the server/ folder:
//     node scripts/create-lecturer.js 900000001 Lecturer@123
//
// Tip: use a 9-digit username so it passes the sign-in form's
// "student number" validation (if your SignInActivity checks for 9 digits).

const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '..', '.env') });

const bcrypt = require('bcrypt');
const pool = require('../db');

async function main() {
    const [username, password] = process.argv.slice(2);

    if (!username || !password || password.length < 8) {
        console.error('Usage: node scripts/create-lecturer.js <username> <password (8+ chars)>');
        process.exit(1);
    }

    const [existing] = await pool.query(
        'SELECT account_id FROM accounts WHERE username = ?',
        [username]
    );
    if (existing.length > 0) {
        console.error(`An account with username "${username}" already exists.`);
        await pool.end();
        process.exit(1);
    }

    const hash = await bcrypt.hash(password, 10);
    const [result] = await pool.query(
        "INSERT INTO accounts (username, password_hash, role, is_active) VALUES (?, ?, 'LECTURER', TRUE)",
        [username, hash]
    );

    console.log(`Lecturer created. account_id=${result.insertId}, username=${username}`);
    await pool.end();
}

main().catch((err) => {
    console.error('Failed to create lecturer:', err.message);
    process.exit(1);
});
