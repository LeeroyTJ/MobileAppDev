const pool = require('../db');

async function runChallenge1Test() {
    console.log('Starting Challenge 1 Concurrency Test (20 iterations)...');
    let successCount = 0;
    let anomalyCount = 0;
    let errorCount = 0;

    for (let i = 1; i <= 20; i++) {
        const connection = await pool.getConnection();
        try {
            await connection.beginTransaction();

            // Create a test group with capacity 15
            const [groupResult] = await connection.query(
                'INSERT INTO lab_groups (name, capacity) VALUES (?, 15)',
                [`Test Group ${i}`]
            );
            const groupId = groupResult.insertId;

            // Create accounts and students for this test
            const [accResult1] = await connection.query(
                'INSERT INTO accounts (student_number, password_hash, role) VALUES (?, ?, ?)',
                [`9999${i}1`, 'hash', 'STUDENT']
            );
            const accId1 = accResult1.insertId;

            const [accResult2] = await connection.query(
                'INSERT INTO accounts (student_number, password_hash, role) VALUES (?, ?, ?)',
                [`9999${i}2`, 'hash', 'STUDENT']
            );
            const accId2 = accResult2.insertId;

            // Fill 14 members into the group
            for (let m = 0; m < 14; m++) {
                const [accM] = await connection.query(
                    'INSERT INTO accounts (student_number, password_hash, role) VALUES (?, ?, ?)',
                    [`8888${i}${m}`, 'hash', 'STUDENT']
                );
                await connection.query(
                    'INSERT INTO students (account_id, name, student_number, programme, group_id) VALUES (?, ?, ?, ?, ?)',
                    [accM.insertId, `Member ${m}`, `8888${i}${m}`, 'CS', groupId]
                );
            }

            // Now group has exactly 14 members!
            // Two unassigned students trying to get the 15th (last) spot simultaneously:
            const [u1] = await connection.query(
                'INSERT INTO students (account_id, name, student_number, programme, group_id) VALUES (?, ?, ?, ?, NULL)',
                [accId1, `Unassigned 1`, `7777${i}1`, 'CS']
            );
            const studentId1 = u1.insertId;

            const [u2] = await connection.query(
                'INSERT INTO students (account_id, name, student_number, programme, group_id) VALUES (?, ?, ?, ?, NULL)',
                [accId2, `Unassigned 2`, `7777${i}2`, 'CS']
            );
            const studentId2 = u2.insertId;

            await connection.commit();
            connection.release();

            // Simulate concurrent assignment requests using SELECT ... FOR UPDATE transaction logic
            const assignStudent = async (sId) => {
                const conn = await pool.getConnection();
                try {
                    await conn.beginTransaction();
                    const [gRows] = await conn.query('SELECT * FROM lab_groups WHERE group_id = ? FOR UPDATE', [groupId]);
                    const grp = gRows[0];
                    const [[{ count }]] = await conn.query('SELECT COUNT(*) as count FROM students WHERE group_id = ? AND deleted_at IS NULL', [groupId]);

                    if (count >= grp.capacity) {
                        await conn.rollback();
                        return { success: false, code: 'GROUP_FULL' };
                    }
                    await conn.query('UPDATE students SET group_id = ? WHERE student_id = ?', [groupId, sId]);
                    await conn.commit();
                    return { success: true };
                } catch (err) {
                    await conn.rollback();
                    return { success: false, error: err.message };
                } finally {
                    conn.release();
                }
            };

            // Run both assignments simultaneously
            const results = await Promise.all([
                assignStudent(studentId1),
                assignStudent(studentId2)
            ]);

            const successes = results.filter(r => r.success).length;
            const fulls = results.filter(r => !r.success && r.code === 'GROUP_FULL').length;

            if (successes === 1 && fulls === 1) {
                successCount++;
            } else {
                anomalyCount++;
                console.log(`Iteration ${i} anomaly: successes=${successes}, fulls=${fulls}`);
            }

        } catch (e) {
            errorCount++;
            console.error(`Iteration ${i} error:`, e.message);
            if (connection) connection.release();
        }
    }

    console.log(`\n--- Challenge 1 Concurrency Test Results ---`);
    console.log(`Total Iterations: 20`);
    console.log(`Successful Isolation (1 Success, 1 GROUP_FULL): ${successCount}`);
    console.log(`Anomalies: ${anomalyCount}`);
    console.log(`Errors: ${errorCount}`);
    process.exit(successCount === 20 ? 0 : 1);
}

runChallenge1Test();
