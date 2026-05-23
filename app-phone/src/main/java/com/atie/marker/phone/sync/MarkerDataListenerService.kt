package com.atie.marker.phone.sync

import com.atie.marker.phone.MarkerPhoneApp
import com.atie.marker.shared.MarkerDataLayerContract
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MarkerDataListenerService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            val path = event.dataItem.uri.path ?: return@forEach
            if (event.type != DataEvent.TYPE_CHANGED) {
                return@forEach
            }
            if (!path.startsWith(MarkerDataLayerContract.MARKER_PATH_PREFIX)) {
                return@forEach
            }
            val sourceNodeId = event.dataItem.uri.host
            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
            val marker = dataMap.toMarkerEvent()
            val app = application as MarkerPhoneApp
            scope.launch {
                app.markerRepository.ingestMarker(marker)
                if (sourceNodeId != null) {
                    acknowledge(sourceNodeId, marker.id, marker.updatedAtEpochMillis)
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) = Unit

    private fun acknowledge(nodeId: String, markerId: String, updatedAtEpochMillis: Long) {
        runCatching {
            Tasks.await(
                Wearable.getMessageClient(this).sendMessage(
                    nodeId,
                    MarkerDataLayerContract.ACK_PATH,
                    "$markerId|$updatedAtEpochMillis".encodeToByteArray(),
                ),
            )
        }
    }
}
