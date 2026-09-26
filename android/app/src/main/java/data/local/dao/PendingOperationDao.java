package data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import data.local.entity.PendingOperationEntity;

import java.util.List;

@Dao
public interface PendingOperationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PendingOperationEntity operation);

    @Update
    void update(PendingOperationEntity operation);

    @Query("SELECT * FROM pending_operations WHERE accountId = :accountId " +
            "AND status IN ('PENDING', 'FAILED') ORDER BY createdAt ASC")
    List<PendingOperationEntity> getReplayQueue(long accountId);

    @Query("SELECT * FROM pending_operations WHERE operationId = :operationId LIMIT 1")
    PendingOperationEntity findById(String operationId);

    @Query("SELECT * FROM pending_operations WHERE studentLocalId = :studentLocalId " +
            "ORDER BY createdAt ASC")
    List<PendingOperationEntity> findForStudent(Long studentLocalId);

    @Query("SELECT * FROM pending_operations WHERE accountId = :accountId ORDER BY createdAt ASC")
    LiveData<List<PendingOperationEntity>> observeQueueForAccount(long accountId);

    @Query("UPDATE pending_operations SET status = :status WHERE operationId = :operationId")
    void updateStatus(String operationId, String status);

    @Query("DELETE FROM pending_operations WHERE accountId = :accountId")
    void clearForAccount(long accountId);

    @Query("DELETE FROM pending_operations WHERE operationId = :operationId")
    void deleteById(String operationId);
}

