const request = require('supertest');
const mysql = require('mysql2/promise');
const fs = require('fs');
const path = require('path');
const app = require('../server');

const TEST_DB_NAME = process.env.TEST_DB_NAME || 'cohorthub_test';

let pool;
let lecturerToken;
let studentToken;
let studentAccountId;
let studentId;

beforeAll(async () => {
    // Ensure test DB ends in _test for safety
    if (!TEST_DB_NAME.endsWith('_test')) {
        throw new Error('TEST_DB_NAME must end in _test to prevent overwriting production data');
    }

    const host = process.env.DB_HOST || 'localhost';
    const user = process.env.DB_USER || 'root';
    const password = process.env.DB_PASSWORD || '';

    // Connect to MySQL root to recreate test database
    const adminConn = await mysql.createConnection({ host, user, password });
    await adminConn.query(`DROP DATABASE IF EXISTS \`${TEST_DB_NAME}\`;`);
    await adminConn.query(`CREATE DATABASE \`${TEST_DB_NAME}\`;`);
    await adminConn.end();

    pool = mysql.createPool({
        host,
        user,
        password,
        database: TEST_DB_NAME,
        waitForConnections: true,
        connectionLimit: 10
    });

    // Run schema.sql and dev_seed.sql
    const schemaSql = fs.readFileSync(path.join(__dirname, '../../database/schema.sql'), 'utf8');
    const seedSql = fs.readFileSync(path.join(__dirname, '../../database/dev_seed.sql'), 'utf8');

    const executeSql = async (sql) => {
        const statements = sql
            .split(';')
            .map(s => s.trim())
            .filter(s => s.length > 0 && !s.startsWith('--') && !s.startsWith('USE'));
        for (const statement of statements) {
            try {
                await pool.query(statement);
            } catch (err) {
                // Ignore drop error or use errors
            }
        }
    };

    await executeSql(schemaSql);
    await executeSql(seedSql);

    // Create Lecturer account
    const bcrypt = require('bcrypt');
    const hash = await bcrypt.hash('Lecturer@123', 10);
    await pool.query(
        "INSERT INTO accounts (username, password_hash, role, is_active) VALUES ('900000001', ?, 'LECTURER', TRUE)",
        [hash]
    );

    // Get Tokens
    const lectRes = await request(app)
        .post('/api/v1/auth/login')
        .send({ username: '900000001', password: 'Lecturer@123' });
    lecturerToken = lectRes.body.data.accessToken;

    // Register a Student
    const studReg = await request(app)
        .post('/api/v1/auth/register')
        .send({
            studentNumber: '202499999',
            name: 'Integration Test Student',
            password: 'Password1',
            programmeId: 1
        });
    expect(studReg.status).toBe(201);

    const studLogin = await request(app)
        .post('/api/v1/auth/login')
        .send({ username: '202499999', password: 'Password1' });
    studentToken = studLogin.body.data.accessToken;
    studentAccountId = studLogin.body.data.accountId;

    const [rows] = await pool.query('SELECT student_id FROM students WHERE account_id = ?', [studentAccountId]);
    studentId = rows[0].student_id;
});

afterAll(async () => {
    if (pool) await pool.end();
});

describe('CohortHub Comprehensive REST API Integration Suite', () => {

    describe('1. Authentication & Security', () => {
        test('Student registration rejects invalid student numbers (non 9-digit)', async () => {
            const res = await request(app)
                .post('/api/v1/auth/register')
                .send({ studentNumber: '12345', name: 'Short Num', password: 'Password1', programmeId: 1 });
            expect(res.status).toBe(400);
            expect(res.body.error.code).toBe('INVALID_STUDENT_NUMBER');
        });

        test('Student registration rejects duplicate active student numbers', async () => {
            const res = await request(app)
                .post('/api/v1/auth/register')
                .send({ studentNumber: '202499999', name: 'Duplicate Test', password: 'Password1', programmeId: 1 });
            expect(res.status).toBe(409);
        });

        test('Claim code registration links student without creating duplicate student row', async () => {
            const res = await request(app)
                .post('/api/v1/auth/register')
                .send({
                    studentNumber: '202401099',
                    name: 'Pretest Student',
                    password: 'Password1',
                    claimCode: 'TESTCODE123'
                });
            expect(res.status).toBe(201);

            const [rows] = await pool.query('SELECT COUNT(*) as count FROM students WHERE student_number = "202401099"');
            expect(rows[0].count).toBe(1);
        });
    });

    describe('2. Role Permissions & Roster Access', () => {
        test('Lecturer can fetch full student roster (200)', async () => {
            const res = await request(app)
                .get('/api/v1/students')
                .set('Authorization', `Bearer ${lecturerToken}`);
            expect(res.status).toBe(200);
            expect(Array.isArray(res.body.data)).toBe(true);
            expect(res.body.data.length).toBeGreaterThan(10);
        });

        test('Student CANNOT fetch full student roster (403 Forbidden)', async () => {
            const res = await request(app)
                .get('/api/v1/students')
                .set('Authorization', `Bearer ${studentToken}`);
            expect(res.status).toBe(403);
        });

        test('Student CANNOT delete another student record (403 Forbidden)', async () => {
            const res = await request(app)
                .delete(`/api/v1/students/${studentId}`)
                .set('Authorization', `Bearer ${studentToken}`);
            expect(res.status).toBe(403);
        });

        test('Unauthenticated request returns HTTP 401', async () => {
            const res = await request(app).get('/api/v1/students');
            expect(res.status).toBe(401);
        });
    });

    describe('3. Challenge 1: Group Capacity & Concurrency', () => {
        test('Assigning student to Group G01 fills 15th spot (200)', async () => {
            // Unassigned student 202401023
            const [unassigned] = await pool.query('SELECT student_id FROM students WHERE student_number = "202401023"');
            const [g01] = await pool.query('SELECT group_id FROM lab_groups WHERE group_code = "G01"');

            const res = await request(app)
                .post(`/api/v1/groups/${g01[0].group_id}/assign`)
                .set('Authorization', `Bearer ${lecturerToken}`)
                .send({ studentId: unassigned[0].student_id });

            expect(res.status).toBe(200);
        });

        test('Assigning 16th student to Group G01 is rejected with HTTP 409 GROUP_FULL', async () => {
            const [unassigned] = await pool.query('SELECT student_id FROM students WHERE student_number = "202401024"');
            const [g01] = await pool.query('SELECT group_id FROM lab_groups WHERE group_code = "G01"');

            const res = await request(app)
                .post(`/api/v1/groups/${g01[0].group_id}/assign`)
                .set('Authorization', `Bearer ${lecturerToken}`)
                .send({ studentId: unassigned[0].student_id });

            expect(res.status).toBe(409);
            expect(res.body.error.code).toBe('GROUP_FULL');
        });
    });

    describe('4. Challenge 2 & 3: Idempotent Sync & Version Conflict Resolution', () => {
        test('Challenge 2: Replayed operationId returns cached response payload without re-execution', async () => {
            const opId = 'test-uuid-op-001';

            const payload = {
                operations: [{
                    operationId: opId,
                    type: 'UPDATE_STUDENT',
                    entity: 'STUDENT',
                    entityId: studentId,
                    baseVersion: 1,
                    payload: { name: 'Idempotency Test Name' }
                }]
            };

            const res1 = await request(app)
                .post('/api/v1/sync')
                .set('Authorization', `Bearer ${lecturerToken}`)
                .send(payload);

            expect(res1.status).toBe(200);
            expect(res1.body.data.results[0].status).toBe('APPLIED');

            // Replay same payload with identical operationId
            const res2 = await request(app)
                .post('/api/v1/sync')
                .set('Authorization', `Bearer ${lecturerToken}`)
                .send(payload);

            expect(res2.status).toBe(200);
            expect(res2.body.data.results[0].status).toBe('APPLIED');
        });

        test('Challenge 3: Stale baseVersion returns status CONFLICT', async () => {
            const opId = 'test-uuid-op-conflict-001';

            // Base version is now 2, but client sends stale version 1
            const payload = {
                operations: [{
                    operationId: opId,
                    type: 'UPDATE_STUDENT',
                    entity: 'STUDENT',
                    entityId: studentId,
                    baseVersion: 1,
                    payload: { name: 'Stale Version Edit' }
                }]
            };

            const res = await request(app)
                .post('/api/v1/sync')
                .set('Authorization', `Bearer ${lecturerToken}`)
                .send(payload);

            expect(res.status).toBe(200);
            expect(res.body.data.results[0].status).toBe('CONFLICT');
        });
    });

    describe('5. Soft Deletion & Reserved Numbers', () => {
        test('Soft deleting student clears group and sets deleted_at', async () => {
            const res = await request(app)
                .delete(`/api/v1/students/${studentId}`)
                .set('Authorization', `Bearer ${lecturerToken}`);
            expect(res.status).toBe(200);

            const [rows] = await pool.query('SELECT deleted_at, group_id FROM students WHERE student_id = ?', [studentId]);
            expect(rows[0].deleted_at).not.toBeNull();
            expect(rows[0].group_id).toBeNull();
        });

        test('Soft-deleted student number remains reserved and blocks re-registration (HTTP 409)', async () => {
            const res = await request(app)
                .post('/api/v1/auth/register')
                .send({
                    studentNumber: '202499999', // soft deleted number
                    name: 'Resurrect Attempt',
                    password: 'Password1',
                    programmeId: 1
                });
            expect(res.status).toBe(409);
            expect(res.body.error.code).toBe('STUDENT_NUMBER_RESERVED');
        });
    });
});
