package data.local;
import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import data.local.dao.PendingOperationDao;
import data.local.dao.StudentDao;
import data.local.entity.PendingOperationEntity;
import data.local.entity.StudentEntity;

@Database(
                entities = {StudentEntity.class, PendingOperationEntity.class},
                version = 1,
                exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract StudentDao studentDao();

    public abstract PendingOperationDao pendingOperationDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "cohorthub.db")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}