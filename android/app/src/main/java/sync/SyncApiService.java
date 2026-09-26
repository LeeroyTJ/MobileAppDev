package sync;

import data.remote.SyncApiModels.SyncRequest;
import data.remote.SyncApiModels.SyncResponseEnvelope;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SyncApiService {

    @POST("/api/v1/sync")
    Call<SyncResponseEnvelope> pushOperations(@Body SyncRequest request);
}
