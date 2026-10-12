package com.example.basecompose.utils.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface giám sát trạng thái kết nối mạng toàn cục.
 */
interface NetworkMonitor {
    /**
     * Flow phát ra true nếu có Internet, false nếu mất kết nối.
     */
    val isConnected: Flow<Boolean>

    /**
     * Kiểm tra trạng thái kết nối mạng tức thời tại thời điểm gọi.
     */
    fun isCurrentlyConnected(): Boolean
}

/**
 * Triển khai giám sát kết nối mạng sử dụng ConnectivityManager.NetworkCallback.
 */
@Singleton
class LiveNetworkMonitor @Inject constructor(
    @ApplicationContext context: Context
) : NetworkMonitor {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    override val isConnected: Flow<Boolean> = callbackFlow {
        if (connectivityManager == null) {
            trySend(false)
            close()
            return@callbackFlow
        }

        // Phát ra trạng thái hiện tại ngay khi bắt đầu lắng nghe
        trySend(isCurrentlyConnected())

        // Theo dõi DEFAULT network (network app thực sự dùng) để trạng thái luôn nhất quán
        // với isCurrentlyConnected(), tránh nhấp nháy khi một network phụ thay đổi.
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(networkCapabilities.hasValidatedInternet())
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        connectivityManager.registerDefaultNetworkCallback(callback)

        awaitClose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
                // Tránh crash nếu callback đã bị unregister trước đó
            }
        }
    }.distinctUntilChanged().conflate()

    override fun isCurrentlyConnected(): Boolean {
        val manager = connectivityManager ?: return false
        val activeNetwork = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasValidatedInternet()
    }

    private fun NetworkCapabilities.hasValidatedInternet(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
