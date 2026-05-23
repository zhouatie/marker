package com.atie.marker.watch.sync

import android.content.Context
import com.atie.marker.shared.MarkerDataLayerContract
import com.atie.marker.shared.MarkerEvent
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

class MarkerDataClientSyncer(
    context: Context,
) : MarkerSyncer {
    private val dataClient = Wearable.getDataClient(context)

    override suspend fun sync(marker: MarkerEvent): Boolean {
        return runCatching {
            val request = PutDataMapRequest.create(MarkerDataLayerContract.markerPath(marker.id))
            request.dataMap.putAll(marker.toDataMap())
            request.setUrgent()
            Tasks.await(dataClient.putDataItem(request.asPutDataRequest()))
        }.isSuccess
    }
}
