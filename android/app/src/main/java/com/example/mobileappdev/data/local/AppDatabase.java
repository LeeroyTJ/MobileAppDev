package com.example.mobileappdev.data.local;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.mobileappdev.data.local.dao.PendingOperationDao;
import com.example.mobileappdev.data.local.dao.StudentDao;
import com.example.mobileappdev.data.local.entity.PendingOperationEntity;
import com.example.mobileappdev.data.local.entity.StudentEntity;

@Database(
        entities = {StudentEntity.class, PendingOperationEntity.class},
        version = 2,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract StudentDao studentDao();

    public abstract PendingOperationDao pendingOperationDao();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Room migration script from version 1 to version 2
            database.execSQL("CREATE TABLE IF NOT EXISTS pending_operations_new (" +
                    "operationId TEXT PRIMARY KEY NOT NULL, " +
                    "accountId INTEGER NOT NULL DEFAULT 0, " +
                    "operationType TEXT, " +
                    "payloadJson TEXT, " +
                    "baseVersion INTEGER NOT NULL DEFAULT 0, " +
                    "status TEXT, " +
                    "createdAt INTEGER NOT NULL DEFAULT 0, " +
                    "retryCount INTEGER NOT NULL DEFAULT 0, " +
                    "studentLocalId INTEGER, " +
                    "serverResultJson TEXT, " +
                    "entityId INTEGER)");
            
            database.execSQL("INSERT OR IGNORE INTO pending_operations_new " +
                    "SELECT operationId, COALESCE(accountId, 0), operationType, payloadJson, " +
                    "COALESCE(baseVersion, 0), status, COALESCE(createdAt, 0), " +
                    "COALESCE(retryCount, 0), studentLocalId, serverResultJson, entityId " +
                    "FROM pending_operations");
            
            database.execSQL("DROP TABLE IF EXISTS pending_operations");
            database.execSQL("ALTER TABLE pending_operations_new RENAME TO pending_operations");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "cohorthub.db")
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
