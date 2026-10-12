package com.example.mobileappdev.adapter;

import static org.junit.Assert.assertEquals;

import com.example.mobileappdev.data.local.entity.PendingOperationEntity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PendingOperationAdapterTest {

    @Test
    public void getOperationTypeLabel_returnsReadableName() {
        assertEquals("New Student", PendingOperationAdapter.getOperationTypeLabel(PendingOperationEntity.TYPE_CREATE_STUDENT));
        assertEquals("Student Edit", PendingOperationAdapter.getOperationTypeLabel(PendingOperationEntity.TYPE_UPDATE_STUDENT));
        assertEquals("Delete Request", PendingOperationAdapter.getOperationTypeLabel(PendingOperationEntity.TYPE_DELETE_STUDENT));
        assertEquals("Student Number Correction", PendingOperationAdapter.getOperationTypeLabel(PendingOperationEntity.TYPE_CORRECT_STUDENT_NUMBER));
        assertEquals("Group Change Request", PendingOperationAdapter.getOperationTypeLabel(PendingOperationEntity.TYPE_REQUEST_GROUP_CHANGE));
        assertEquals("Group Transfer", PendingOperationAdapter.getOperationTypeLabel(PendingOperationEntity.TYPE_TRANSFER_GROUP));
    }

    @Test
    public void extractStudentInfo_parsesNameAndNumberFromPayload() {
        PendingOperationEntity op = new PendingOperationEntity();
        op.payloadJson = "{\"studentName\":\"Thabo Mbeki\",\"studentNumber\":\"202401001\",\"group_code\":\"G01\"}";

        String info = PendingOperationAdapter.extractStudentInfo(op);
        assertEquals("Thabo Mbeki (202401001) • Group: G01", info);
    }

    @Test
    public void extractStudentInfo_parsesCorrectedStudentNumber() {
        PendingOperationEntity op = new PendingOperationEntity();
        op.payloadJson = "{\"correctedStudentNumber\":\"202409999\",\"name\":\"Alice\"}";

        String info = PendingOperationAdapter.extractStudentInfo(op);
        assertEquals("Corrected #: 202409999 (Alice)", info);
    }

    @Test
    public void getStatusLabel_returnsReadableStatus() {
        assertEquals("Pending", PendingOperationAdapter.getStatusLabel(PendingOperationEntity.STATUS_PENDING));
        assertEquals("Syncing...", PendingOperationAdapter.getStatusLabel(PendingOperationEntity.STATUS_SYNCING));
        assertEquals("Synced", PendingOperationAdapter.getStatusLabel(PendingOperationEntity.STATUS_SYNCED));
        assertEquals("Action Required", PendingOperationAdapter.getStatusLabel(PendingOperationEntity.STATUS_CONFLICT));
    }
}
