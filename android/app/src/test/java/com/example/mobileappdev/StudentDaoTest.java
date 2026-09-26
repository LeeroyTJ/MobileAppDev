package com.example.mobileappdev;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import data.local.AppDatabase;
import data.local.entity.StudentEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;

@RunWith(RobolectricTestRunner.class)
public class StudentDaoTest {

    private AppDatabase db;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void closeDb() throws IOException {
        db.close();
    }

    @Test
    public void insertAndReadStudent() {
        StudentEntity s = new StudentEntity();
        s.accountId = 1L;
        s.name = "Test Student";
        s.studentNumber = "000123456";
        s.programme = "CS";
        s.syncStatus = StudentEntity.STATUS_SAVED_LOCALLY;

        long id = db.studentDao().insert(s);
        StudentEntity fetched = db.studentDao().findByLocalId(id);

        assertNotNull(fetched);
        assertEquals("Test Student", fetched.name);
        assertEquals("000123456", fetched.studentNumber);
    }
}

