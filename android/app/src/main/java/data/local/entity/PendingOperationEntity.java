package data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pending_operations")
public class PendingOperationEntity {

    @PrimaryKey
    @NonNull
    public String operationId;

    public Long entityId;

    public Long studentLocalId;

    @NonNull
    public Long accountId;

    @NonNull
    public String operationType;

    @NonNull
    public String payloadJson;

    public int baseVersion;

    @NonNull
    public String status;

    public long createdAt;

    public String serverResultJson;

    public int retryCount;

    public PendingOperationEntity() {
    }

    public static final String TYPE_CREATE_STUDENT = "CREATE_STUDENT";
    public static final String TYPE_UPDATE_STUDENT = "UPDATE_STUDENT";
    public static final String TYPE_DELETE_STUDENT = "DELETE_STUDENT";
    public static final String TYPE_CORRECT_STUDENT_NUMBER = "CORRECT_STUDENT_NUMBER";
    public static final String TYPE_ASSIGN_GROUP = "ASSIGN_GROUP";
    public static final String TYPE_TRANSFER_GROUP = "TRANSFER_GROUP";
    public static final String TYPE_REQUEST_GROUP_CHANGE = "REQUEST_GROUP_CHANGE";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SYNCING = "SYNCING";
    public static final String STATUS_SYNCED = "SYNCED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CONFLICT = "CONFLICT";
    public static final String STATUS_REJECTED = "REJECTED";
}


