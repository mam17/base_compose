package com.example.basecompose.base

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.basecompose.R
import com.example.basecompose.ui.theme.BaseComposeTheme
import com.example.basecompose.utils.extensions.openNetworkSettings
import com.example.basecompose.utils.network.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Trạng thái mạng hiện tại, đọc được ở bất kỳ Composable nào bên trong [BaseApp]:
 * ```kotlin
 * val isOnline = LocalIsOnline.current
 * ```
 */
val LocalIsOnline = staticCompositionLocalOf { true }

/**
 * Composable gốc của toàn app — thay cho BaseActivity. Activity chỉ cần gọi:
 * ```kotlin
 * @AndroidEntryPoint
 * class MainActivity : ComponentActivity() {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         enableEdgeToEdge()
 *         super.onCreate(savedInstanceState)
 *         setContent { BaseApp { AppNavHost() } }
 *     }
 * }
 * ```
 *
 * Tự động:
 * - Bọc nội dung trong [BaseComposeTheme].
 * - Theo dõi mạng toàn cục, cung cấp [LocalIsOnline], hiển thị [NetworkDialog] khi mất mạng.
 *
 * @param checkNetwork tự hiển thị [NetworkDialog] khi mất mạng; false nếu app chạy offline
 */
@Composable
fun BaseApp(
    checkNetwork: Boolean = true,
    networkViewModel: NetworkViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    BaseComposeTheme {
        val isOnline by networkViewModel.isOnline.collectAsStateWithLifecycle()
        val context = LocalContext.current

        CompositionLocalProvider(LocalIsOnline provides isOnline) {
            content()

            if (checkNetwork && !isOnline) {
                NetworkDialog(
                    onRetry = {
                        if (!networkViewModel.isCurrentlyConnected()) {
                            Toast.makeText(context, R.string.network_still_offline, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOpenSettings = context::openNetworkSettings,
                )
            }
        }
    }
}

/** Cầu nối [NetworkMonitor] (Hilt) với Compose. */
@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = networkMonitor.isConnected.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = networkMonitor.isCurrentlyConnected(),
    )

    fun isCurrentlyConnected(): Boolean = networkMonitor.isCurrentlyConnected()
}
