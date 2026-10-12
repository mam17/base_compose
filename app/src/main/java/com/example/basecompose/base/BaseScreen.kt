package com.example.basecompose.base

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.basecompose.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Khung chung cho MỌI màn hình Compose — thay cho BaseActivity/BaseFragment của View system.
 *
 * Compose ưu tiên composition thay vì kế thừa, nên thay vì `class XxxScreen : BaseScreen`,
 * mỗi màn gồm:
 * 1. `XxxRoute` — route `@Serializable` cho Navigation (khai báo trong `XxxNavigation.kt`).
 * 2. `XxxViewModel : BaseViewModel<XxxState, XxxEvent, XxxEffect>` (kế thừa).
 * 3. `XxxScreen` (stateful) bọc trong [BaseScreen] — nhận ViewModel, chuyển effect thành callback điều hướng.
 * 4. `XxxContent` (stateless) — chỉ nhận `state` + `onEvent`, dễ Preview và test.
 *
 * ViewModel KHÔNG biết NavController: nó gửi effect, `XxxScreen` gọi callback,
 * graph (`XxxNavigation.kt`) mới thực sự gọi `navController.navigate(...)`.
 *
 * [BaseScreen] tự động:
 * - Collect `uiState` theo lifecycle (dừng khi app xuống background).
 * - Collect `effect` một lần, an toàn lifecycle -> gọi [onEffect].
 * - Hiển thị loading overlay, dialog lỗi, Snackbar từ [BaseViewModel].
 * - Cung cấp [Scaffold] (topBar, bottomBar, FAB) + insets edge-to-edge.
 * - Xử lý nút Back nếu truyền [onBack].
 *
 * Ví dụ:
 * ```kotlin
 * @Composable
 * fun HomeScreen(
 *     onOpenDetail: (String) -> Unit,
 *     viewModel: HomeViewModel = hiltViewModel(),
 * ) {
 *     BaseScreen(
 *         viewModel = viewModel,
 *         onEffect = { effect ->
 *             when (effect) {
 *                 is HomeEffect.OpenDetail -> onOpenDetail(effect.id)
 *             }
 *         },
 *         topBar = { TopAppBar(title = { Text("Home") }) },
 *     ) { state, onEvent, padding ->
 *         HomeContent(state, onEvent, Modifier.padding(padding))
 *     }
 * }
 * ```
 *
 * @param viewModel ViewModel của màn, kế thừa [BaseViewModel]
 * @param onEffect xử lý effect một lần (navigate, toast, mở intent...)
 * @param onBack xử lý nút Back hệ thống; null = để hệ thống/Navigation tự xử lý
 * @param showLoadingOverlay tự hiển thị loading toàn màn khi `commonState.isLoading`
 * @param showErrorDialog tự hiển thị dialog lỗi khi `commonState.error != null`
 * @param content nội dung màn hình, nhận `state`, `onEvent` và `contentPadding` của Scaffold
 */
@Composable
fun <S : UiState, E : UiEvent, F : UiEffect> BaseScreen(
    viewModel: BaseViewModel<S, E, F>,
    modifier: Modifier = Modifier,
    onEffect: suspend (F) -> Unit = {},
    onBack: (() -> Unit)? = null,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    showLoadingOverlay: Boolean = true,
    showErrorDialog: Boolean = true,
    content: @Composable (state: S, onEvent: (E) -> Unit, contentPadding: PaddingValues) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val commonState by viewModel.commonState.collectAsStateWithLifecycle()
    val onEvent: (E) -> Unit = remember(viewModel) { viewModel::onEvent }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    ObserveAsEvents(viewModel.effect) { onEffect(it) }
    ObserveAsEvents(viewModel.messages) { message ->
        snackbarHostState.showSnackbar(message.asString(context))
    }

    if (onBack != null) {
        BackHandler(onBack = onBack)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = topBar,
            bottomBar = bottomBar,
            floatingActionButton = floatingActionButton,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = containerColor,
            contentWindowInsets = contentWindowInsets,
        ) { padding ->
            content(state, onEvent, padding)
        }

        if (showLoadingOverlay && commonState.isLoading) {
            LoadingOverlay()
        }
    }

    val error = commonState.error
    if (showErrorDialog && error != null) {
        BaseAlertDialog(
            title = stringResource(R.string.error_title),
            message = error.asString(),
            confirmText = stringResource(R.string.common_ok),
            dismissText = null,
            onConfirm = viewModel::dismissError,
            onDismiss = viewModel::dismissError,
        )
    }
}

/**
 * Collect một Flow sự kiện một lần (effect) an toàn theo lifecycle.
 *
 * - Chỉ collect khi lifecycle >= STARTED, tự dừng khi xuống background.
 * - Dùng `Dispatchers.Main.immediate` để không bỏ lỡ sự kiện giữa các lần chuyển trạng thái.
 * - [onEvent] luôn là phiên bản mới nhất (rememberUpdatedState), không restart việc collect.
 */
@Composable
fun <T> ObserveAsEvents(
    flow: Flow<T>,
    vararg keys: Any?,
    onEvent: suspend (T) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEvent by rememberUpdatedState(onEvent)
    LaunchedEffect(flow, lifecycleOwner, *keys) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) {
                flow.collect { currentOnEvent(it) }
            }
        }
    }
}

/** Loading toàn màn: lớp phủ mờ, chặn mọi thao tác chạm phía dưới. */
@Composable
fun LoadingOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
