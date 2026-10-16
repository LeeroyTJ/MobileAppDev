package com.example.mobileappdev.data.repository;

import com.example.mobileappdev.data.local.AppDatabase;
import com.example.mobileappdev.data.local.dao.PendingOperationDao;
import com.example.mobileappdev.data.local.dao.StudentDao;
import com.example.mobileappdev.data.local.entity.PendingOperationEntity;
import com.example.mobileappdev.data.local.entity.StudentEntity;

import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

public class StudentSyncRepository {

    private final AppDatabase db;
    private final StudentDao studentDao;
    private final PendingOperationDao pendingOperationDao;

    public StudentSyncRepository(AppDatabase db) {
        this.db = db;
        this.studentDao = db.studentDao();
        this.pendingOperationDao = db.pendingOperationDao();
    }

    public void submitCreate(StudentEntity draft, long accountId, String payloadJson) {
        db.runInTransaction(() -> {
            draft.accountId = accountId;
            draft.syncStatus = StudentEntity.STATUS_PENDING;
            long localId = studentDao.insert(draft);

            PendingOperationEntity op = newOp(accountId,
                    PendingOperationEntity.TYPE_CREATE_STUDENT, payloadJson, 0);
            op.studentLocalId = localId;
            pendingOperationDao.insert(op);
        });
    }

    public void submitEdit(long studentLocalId, long accountId, String payloadJson, int baseVersion) {
        db.runInTransaction(() -> {
            StudentEntity student = studentDao.findByLocalId(studentLocalId);
            if (student == null) return;

            student.syncStatus = StudentEntity.STATUS_PENDING;
            studentDao.update(student);

            PendingOperationEntity collapseTarget = findUnsyncedOp(studentLocalId, accountId,
                    PendingOperationEntity.TYPE_CREATE_STUDENT,
                    PendingOperationEntity.TYPE_UPDATE_STUDENT);

            if (collapseTarget != null) {
                collapseTarget.payloadJson = payloadJson;
                pendingOperationDao.update(collapseTarget);
                return;
            }

            PendingOperationEntity op = newOp(accountId,
                    PendingOperationEntity.TYPE_UPDATE_STUDENT, payloadJson, baseVersion);
            op.studentLocalId = studentLocalId;
            op.entityId = student.serverId;
            pendingOperationDao.insert(op);
        });
    }

    public void submitDelete(long studentLocalId, long accountId) {
        db.runInTransaction(() -> {
            StudentEntity student = studentDao.findByLocalId(studentLocalId);
            if (student == null) return;

            List<PendingOperationEntity> existing = pendingOperationDao.findForStudent(studentLocalId, accountId);
            boolean neverLeftDevice = false;
            for (PendingOperationEntity op : existing) {
                if (PendingOperationEntity.STATUS_PENDING.equals(op.status)
                        && PendingOperationEntity.TYPE_CREATE_STUDENT.equals(op.operationType)) {
                    neverLeftDevice = true;
                    break;
                }
            }

            if (neverLeftDevice) {
                for (PendingOperationEntity op : existing) {
                    if (PendingOperationEntity.STATUS_PENDING.equals(op.status)) {
                        pendingOperationDao.deleteById(op.operationId);
                    }
                }
                studentDao.delete(student);
                return;
            }

            student.syncStatus = StudentEntity.STATUS_PENDING;
            studentDao.update(student);

            PendingOperationEntity op = newOp(accountId,
                    PendingOperationEntity.TYPE_DELETE_STUDENT, "{}", student.baseVersion);
            op.studentLocalId = studentLocalId;
            op.entityId = student.serverId;
            pendingOperationDao.insert(op);
        });
    }

    /**
     * Offline-safe queueing for group changes (ASSIGN or TRANSFER).
     * Call from any Activity that changes a student's group.
     */
    public void submitGroupChange(long studentLocalId, long accountId, long serverId,
                                  String newGroup, String currentGroup, int baseVersion) {
        db.runInTransaction(() -> {
            StudentEntity student = studentDao.findByLocalId(studentLocalId);
            if (student == null) return;

            // 1. Update local row immediately
            student.labGroup = newGroup;
            student.syncStatus = StudentEntity.STATUS_PENDING;
            studentDao.update(student);

            // 2. Queue the operation (collapse if one is already pending for this student)
            PendingOperationEntity collapseTarget = findUnsyncedOp(studentLocalId, accountId,
                    PendingOperationEntity.TYPE_ASSIGN_GROUP,
                    PendingOperationEntity.TYPE_TRANSFER_GROUP);
            if (collapseTarget != null) {
                collapseTarget.payloadJson = buildGroupPayload(serverId, newGroup);
                pendingOperationDao.update(collapseTarget);
                return;
            }

            boolean wasUnassigned = currentGroup == null
                    || currentGroup.isEmpty()
                    || "Unassigned".equalsIgnoreCase(currentGroup);
            String opType = wasUnassigned
                    ? PendingOperationEntity.TYPE_ASSIGN_GROUP
                    : PendingOperationEntity.TYPE_TRANSFER_GROUP;

            PendingOperationEntity op = newOp(accountId, opType,
                    buildGroupPayload(serverId, newGroup), baseVersion);
            op.studentLocalId = studentLocalId;
            op.entityId = serverId;
            pendingOperationDao.insert(op);
        });
    }

    /**
     * Offline-safe queueing for student number corrections.
     */
    public void submitNumberCorrection(long studentLocalId, long accountId, long serverId,
                                       String correctedNumber, int baseVersion) {
        db.runInTransaction(() -> {
            StudentEntity student = studentDao.findByLocalId(studentLocalId);
            if (student == null) return;

            student.studentNumber = correctedNumber;
            student.syncStatus = StudentEntity.STATUS_PENDING;
            studentDao.update(student);

            PendingOperationEntity op = newOp(accountId,
                    PendingOperationEntity.TYPE_CORRECT_STUDENT_NUMBER,
                    buildNumberPayload(serverId, correctedNumber),
                    baseVersion);
            op.studentLocalId = studentLocalId;
            op.entityId = serverId;
            pendingOperationDao.insert(op);
        });
    }

    /**
     * Offline-safe queueing for name-only edits.
     * Reuses submitEdit() since the payload shape is the same.
     */
    public void submitNameEdit(long studentLocalId, long accountId, String newName, int baseVersion) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("studentName", newName);
            submitEdit(studentLocalId, accountId, payload.toString(), baseVersion);
        } catch (Exception ignored) { }
    }

    private String buildGroupPayload(long serverId, String newGroup) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("studentId", serverId);
            payload.put("labGroup", newGroup);
            payload.put("targetGroup", newGroup);
            return payload.toString();
        } catch (Exception e) {
            return "{\"labGroup\":\"" + newGroup + "\"}";
        }
    }

    private String buildNumberPayload(long serverId, String correctedNumber) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("studentId", serverId);
            payload.put("correctedStudentNumber", correctedNumber);
            return payload.toString();
        } catch (Exception e) {
            return "{\"correctedStudentNumber\":\"" + correctedNumber + "\"}";
        }
    }

    public void applySyncResult(String operationId, String resultStatus, String serverResultJson,
                                Long newEntityId, Integer newVersion) {
        db.runInTransaction(() -> {
            PendingOperationEntity op = pendingOperationDao.findById(operationId);
            if (op == null) return;

            op.serverResultJson = serverResultJson;

            switch (resultStatus) {
                case "APPLIED":
                    op.status = PendingOperationEntity.STATUS_SYNCED;
                    if (op.studentLocalId != null) {
                        StudentEntity student = studentDao.findByLocalId(op.studentLocalId);
                        if (student != null) {
                            if (newEntityId != null)
                                student.serverId = newEntityId;
                            if (newVersion != null)
                                student.baseVersion = newVersion;
                            student.syncStatus = StudentEntity.STATUS_SYNCED;
                            studentDao.update(student);
                        }
                    }
                    break;
                case "CONFLICT":
                    op.status = PendingOperationEntity.STATUS_CONFLICT;
                    markActionRequired(op);
                    break;
                case "REJECTED":
                default:
                    op.status = PendingOperationEntity.STATUS_REJECTED;
                    markActionRequired(op);
                    break;
            }

            pendingOperationDao.update(op);
        });
    }

    private void markActionRequired(PendingOperationEntity op) {
        if (op.studentLocalId == null) return;
        StudentEntity student = studentDao.findByLocalId(op.studentLocalId);
        if (student != null) {
            student.syncStatus = StudentEntity.STATUS_ACTION_REQUIRED;
            studentDao.update(student);
        }
    }

    private PendingOperationEntity newOp(long accountId, String type, String payloadJson, int baseVersion) {
        PendingOperationEntity op = new PendingOperationEntity();
        op.operationId = UUID.randomUUID().toString();
        op.accountId = accountId;
        op.operationType = type;
        op.payloadJson = payloadJson;
        op.baseVersion = baseVersion;
        op.status = PendingOperationEntity.STATUS_PENDING;
        op.createdAt = System.currentTimeMillis();
        op.retryCount = 0;
        return op;
    }

    private PendingOperationEntity findUnsyncedOp(long studentLocalId, long accountId, String... types) {
        List<PendingOperationEntity> ops = pendingOperationDao.findForStudent(studentLocalId, accountId);
        for (PendingOperationEntity op : ops) {
            if (!PendingOperationEntity.STATUS_PENDING.equals(op.status))
                continue;
            for (String type : types) {
                if (type.equals(op.operationType))
                    return op;
            }
        }
        return null;
    }
}