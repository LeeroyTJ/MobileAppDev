package data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 *  * Local cache of a student record. localId is immutable and independent of
 *   * studentNumber. serverId mirrors the contract's numeric studentId and is
 *    * null until a CREATE_STUDENT operation has been APPLIED.
 *     */
@Entity(tableName = "students")
public class StudentEntity {

    @PrimaryKey(autoGenerate = true)
    public long localId;

    public Long serverId; // contract's studentId (integer) - null until synced once

    @NonNull
    public Long accountId; // owner scoping, numeric per contract

    @NonNull
    public String name;

    @NonNull
    public String studentNumber; // 9-digit string, leading zeros preserved

    @NonNull
    public String programme; // CS, IT, DS

    public String labGroup; // G01-G04, or null for Unassigned

    @NonNull
    public String syncStatus; // SAVED_LOCALLY | PENDING | SYNCING | SYNCED | ACTION_REQUIRED

    public int baseVersion; // mirrors contract's "version" field

    public boolean isDeleted; // soft-delete marker mirrored from server

    public long lastUpdatedAt;

    public StudentEntity() {
    }

    public static final String STATUS_SAVED_LOCALLY = "SAVED_LOCALLY";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SYNCING = "SYNCING";
    public static final String STATUS_SYNCED = "SYNCED";
    public static final String STATUS_ACTION_REQUIRED = "ACTION_REQUIRED";
}