package com.example.mobileappdev;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.example.mobileappdev.data.local.AppDatabase;
import com.example.mobileappdev.data.local.entity.PendingOperationEntity;
import com.example.mobileappdev.data.local.entity.StudentEntity;
import com.example.mobileappdev.data.repository.StudentSyncRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class StudentSyncRepositoryTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase db;
    private StudentSyncRepository repo;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        repo = new StudentSyncRepository(db);
    }

    @After
    public void tearDown() {
        db.close();
    }

    private StudentEntity draft(String number) {
        StudentEntity s = new StudentEntity();
        s.name = "Test Student";
        s.studentNumber = number;
        s.programme = "CS";
        s.syncStatus = StudentEntity.STATUS_SAVED_LOCALLY;
        return s;
    }

    private long onlyLocalId(long accountId) {
        List<StudentEntity> list = db.studentDao().findUnsyncedForAccount(accountId);
        assertEquals(1, list.size());
        return list.get(0).localId;
    }

    @Test
    public void create_savesStudentAndQueuesOneOperationTogether() {
        repo.submitCreate(draft("000000001"), 1L, "{}");
        assertEquals(1, db.studentDao().findUnsyncedForAccount(1L).size());
        assertEquals(1, db.pendingOperationDao().getReplayQueue(1L).size());
    }

    @Test
    public void createThenDeleteBeforeSync_sendsNothingToTheServer() {
        repo.submitCreate(draft("000000002"), 1L, "{}");
        long localId = onlyLocalId(1L);
        repo.submitDelete(localId, 1L);
        assertEquals(0, db.pendingOperationDao().getReplayQueue(1L).size());
        assertNull(db.studentDao().findByLocalId(localId));
    }

    @Test
    public void repeatedOfflineEdits_collapseIntoOneOperation() {
        repo.submitCreate(draft("000000003"), 1L, "{\"v\":1}");
        long localId = onlyLocalId(1L);
        repo.submitEdit(localId, 1L, "{\"v\":2}", 0);
        repo.submitEdit(localId, 1L, "{\"v\":3}", 0);
        List<PendingOperationEntity> queue = db.pendingOperationDao().getReplayQueue(1L);
        assertEquals(1, queue.size());
        assertEquals("{\"v\":3}", queue.get(0).payloadJson);
    }

    @Test
    public void conflictResult_marksActionRequired_andStopsAutoRetry() {
        repo.submitCreate(draft("000000004"), 1L, "{}");
        long localId = onlyLocalId(1L);
        String opId = db.pendingOperationDao().getReplayQueue(1L).get(0).operationId;

        repo.applySyncResult(opId, "CONFLICT", "{}", null, null);

        assertEquals(StudentEntity.STATUS_ACTION_REQUIRED, db.studentDao().findByLocalId(localId).syncStatus);
        assertEquals(PendingOperationEntity.STATUS_CONFLICT, db.pendingOperationDao().findById(opId).status);
        assertEquals(0, db.pendingOperationDao().getReplayQueue(1L).size());
    }

    @Test
    public void appliedResult_storesServerIdAndVersion() {
        repo.submitCreate(draft("000000005"), 1L, "{}");
        long localId = onlyLocalId(1L);
        String opId = db.pendingOperationDao().getReplayQueue(1L).get(0).operationId;

        repo.applySyncResult(opId, "APPLIED", "{}", 42L, 1);

        StudentEntity s = db.studentDao().findByLocalId(localId);
        assertEquals(Long.valueOf(42L), s.serverId);
        assertEquals(1, s.baseVersion);
        assertEquals(StudentEntity.STATUS_SYNCED, s.syncStatus);
    }

    @Test
    public void queueIsScopedByAccount_noReplayUnderAnotherUser() {
        repo.submitCreate(draft("000000006"), 1L, "{}");
        repo.submitCreate(draft("000000007"), 2L, "{}");
        assertEquals(1, db.pendingOperationDao().getReplayQueue(1L).size());
        assertEquals(1, db.pendingOperationDao().getReplayQueue(2L).size());
        db.pendingOperationDao().clearForAccount(1L);
        assertEquals(0, db.pendingOperationDao().getReplayQueue(1L).size());
        assertEquals(1, db.pendingOperationDao().getReplayQueue(2L).size());
    }
}
