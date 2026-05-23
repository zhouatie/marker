package com.atie.marker.watch.sync

import com.atie.marker.shared.MarkerDataLayerContract
import com.atie.marker.watch.MarkerWatchApp
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MarkerAckListenerService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != MarkerDataLayerContract.ACK_PATH) {
            return
        }
        val ack = messageEvent.data.decodeToString().split('|')
        val markerId = ack.getOrNull(0) ?: return
        val acknowledgedUpdatedAt = ack.getOrNull(1)?.toLongOrNull() ?: return
        val app = application as MarkerWatchApp
        scope.launch {
            app.markerRepository.markSynced(markerId, acknowledgedUpdatedAt)
        }
    }

    override fun onPeerConnected(peer: Node) {
        val app = application as MarkerWatchApp
        scope.launch {
            app.markerRepository.syncPendingMarkers()
        }
    }
}
