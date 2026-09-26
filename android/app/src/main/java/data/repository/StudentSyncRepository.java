package data.repository;

import data.local.AppDatabase;
import data.local.dao.PendingOperationDao;
import data.local.dao.StudentDao;
import data.local.entity.PendingOperationEntity;
import data.local.entity.StudentEntity;

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

            PendingOperationEntity collapseTarget = findUnsyncedOp(studentLocalId,
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

            List<PendingOperationEntity> existing = pendingOperationDao.findForStudent(studentLocalId);
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

    private PendingOperationEntity findUnsyncedOp(long studentLocalId, String... types) {
        List<PendingOperationEntity> ops = pendingOperationDao.findForStudent(studentLocalId);
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