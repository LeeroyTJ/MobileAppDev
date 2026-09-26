package data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import data.local.entity.StudentEntity;

import java.util.List;

@Dao
public interface StudentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(StudentEntity student);

    @Update
    void update(StudentEntity student);

    @Delete
    void delete(StudentEntity student);

    @Query("SELECT * FROM students WHERE accountId = :accountId AND isDeleted = 0")
    LiveData<List<StudentEntity>> observeAllForAccount(long accountId);

    @Query("SELECT * FROM students WHERE serverId = :serverId LIMIT 1")
    StudentEntity findByServerId(long serverId);

    @Query("SELECT * FROM students WHERE localId = :localId LIMIT 1")
    StudentEntity findByLocalId(long localId);

    @Query("SELECT * FROM students WHERE accountId = :accountId AND labGroup IS NULL")
    LiveData<List<StudentEntity>> observeUnassigned(long accountId);

    @Query("SELECT COUNT(*) FROM students WHERE labGroup = :group AND isDeleted = 0")
    int countActiveInGroup(String group);

    @Query("SELECT * FROM students WHERE accountId = :accountId AND syncStatus != 'SYNCED'")
    List<StudentEntity> findUnsyncedForAccount(long accountId);

    @Query("DELETE FROM students WHERE accountId = :accountId")
    void clearForAccount(long accountId);
}
