package data.remote;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class SyncApiModels {

    public static class SyncRequest {
        public List<SyncOperation> operations;

        public SyncRequest(List<SyncOperation> operations) {
            this.operations = operations;
        }
    }

    public static class SyncOperation {
        public String operationId;
        public String operationType;
        public String entityType;
        public Long entityId;
        public Long accountId;
        public Integer baseVersion;
        public Map<String, Object> payload;
    }

    public static class SyncResponseEnvelope {
        public boolean success;
        public SyncResponseData data;
    }

    public static class SyncResponseData {
        public List<SyncResult> results;
    }

    public static class SyncResult {
        public String operationId;
        public String status;
        public String entityType;
        public Long entityId;
        public Integer version;

        @SerializedName("serverRecord")
        public Map<String, Object> serverRecord;
        @SerializedName("clientVersion")
        public Integer clientVersion;
        @SerializedName("serverVersion")
        public Integer serverVersion;
    }
}



