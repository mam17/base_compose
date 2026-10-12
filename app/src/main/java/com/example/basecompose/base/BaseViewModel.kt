package com.example.basecompose.base

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Trạng thái chung mà mọi màn đều có, được [BaseScreen] tự động hiển thị.
 *
 * @property loadingCount số tác vụ đang chạy với `showLoading = true`.
 *                        Dùng bộ đếm để nhiều request song song không tắt loading của nhau.
 * @property error lỗi đang hiển thị dạng dialog, null nếu không có.
 */
@Immutable
data class CommonUiState(
    val loadingCount: Int = 0,
    val error: UiText? = null,
) {
    val isLoading: Boolean get() = loadingCount > 0
}

/**
 * Base ViewModel theo mô hình MVI cho Compose. Mọi ViewModel của màn hình đều kế thừa class này.
 *
 * Luồng dữ liệu một chiều:
 * ```
 * UI --onEvent(E)--> ViewModel --setState--> uiState: StateFlow<S> --> UI
 *                             --sendEffect--> effect: Flow<F> (one-shot) --> UI
 * ```
 *
 * Tính năng:
 * - [uiState]: state duy nhất của màn, cập nhật nguyên tử qua [setState].
 * - [effect]: sự kiện một lần (navigate, toast...) qua Channel, không bị phát lại khi xoay màn hình.
 * - [commonState]: loading / error toàn màn, [BaseScreen] tự hiển thị.
 * - [messages]: snackbar message, [BaseScreen] tự hiển thị.
 * - [launch] / [launchLatest]: chạy coroutine an toàn, tự bật/tắt loading, tự bắt lỗi,
 *   không nuốt [CancellationException].
 *
 * Quy ước coroutine:
 * - ViewModel chạy trên Main (viewModelScope). Việc chuyển thread (IO/Default) là trách nhiệm
 *   của Repository / UseCase thông qua `withContext(ioDispatcher)` — xem `di/CoroutinesModule.kt`.
 *   Nhờ vậy ViewModel luôn "main-safe" và dễ test với `Dispatchers.setMain`.
 *
 * Ví dụ:
 * ```kotlin
 * @HiltViewModel
 * class HomeViewModel @Inject constructor(
 *     private val repository: UserRepository,
 * ) : BaseViewModel<HomeState, HomeEvent, HomeEffect>(HomeState()) {
 *
 *     init { onEvent(HomeEvent.Refresh) }
 *
 *     override fun onEvent(event: HomeEvent) {
 *         when (event) {
 *             HomeEvent.Refresh -> launch {
 *                 val users = repository.getUsers()
 *                 setState { copy(users = users) }
 *             }
 *             is HomeEvent.UserClicked -> sendEffect(HomeEffect.OpenDetail(event.id))
 *         }
 *     }
 * }
 * ```
 *
 * @param S kiểu [UiState] của màn
 * @param E kiểu [UiEvent] của màn
 * @param F kiểu [UiEffect] của màn
 */
abstract class BaseViewModel<S : UiState, E : UiEvent, F : UiEffect>(
    initialState: S,
) : ViewModel() {

    // ==========================================
    // STATE
    // ==========================================

    private val _uiState = MutableStateFlow(initialState)

    /** State của màn hình. UI collect bằng `collectAsStateWithLifecycle()`. */
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    /** Giá trị state hiện tại, tiện cho việc đọc trong ViewModel. */
    protected val currentState: S get() = _uiState.value

    /**
     * Cập nhật state một cách nguyên tử (thread-safe).
     *
     * ```kotlin
     * setState { copy(isRefreshing = false, items = newItems) }
     * ```
     */
    protected fun setState(reducer: S.() -> S) {
        _uiState.update(reducer)
    }

    // ==========================================
    // EVENT
    // ==========================================

    /** Điểm vào duy nhất cho mọi hành động từ UI. */
    abstract fun onEvent(event: E)

    // ==========================================
    // EFFECT (ONE-SHOT)
    // ==========================================

    private val _effect = Channel<F>(Channel.BUFFERED)

    /**
     * Sự kiện một lần. Dùng Channel để mỗi effect chỉ được xử lý đúng 1 lần,
     * và được giữ lại (buffer) nếu UI đang ở background, xử lý khi UI quay lại STARTED.
     */
    val effect: Flow<F> = _effect.receiveAsFlow()

    protected fun sendEffect(effect: F) {
        _effect.trySend(effect)
    }

    // ==========================================
    // COMMON STATE: LOADING / ERROR / MESSAGE
    // ==========================================

    private val _commonState = MutableStateFlow(CommonUiState())

    /** Loading & error toàn màn hình, được [BaseScreen] hiển thị tự động. */
    val commonState: StateFlow<CommonUiState> = _commonState.asStateFlow()

    private val _messages = Channel<UiText>(Channel.BUFFERED)

    /** Message ngắn hiển thị dạng Snackbar, được [BaseScreen] hiển thị tự động. */
    val messages: Flow<UiText> = _messages.receiveAsFlow()

    /** Hiển thị Snackbar. */
    protected fun showMessage(message: UiText) {
        _messages.trySend(message)
    }

    /** Hiển thị dialog lỗi toàn màn. */
    protected fun showError(error: UiText) {
        _commonState.update { it.copy(error = error) }
    }

    /** Đóng dialog lỗi (được gọi từ [BaseScreen]). */
    fun dismissError() {
        _commonState.update { it.copy(error = null) }
    }

    protected fun showLoading() {
        _commonState.update { it.copy(loadingCount = it.loadingCount + 1) }
    }

    protected fun hideLoading() {
        _commonState.update { it.copy(loadingCount = (it.loadingCount - 1).coerceAtLeast(0)) }
    }

    /**
     * Xử lý lỗi mặc định cho [launch]: hiển thị dialog lỗi.
     * Override để custom (vd: lỗi 401 -> logout, lỗi nhẹ -> snackbar).
     */
    protected open fun handleError(throwable: Throwable) {
        showError(throwable.toUiText())
    }

    // ==========================================
    // COROUTINES
    // ==========================================

    /**
     * Launch coroutine an toàn trong [viewModelScope].
     *
     * - Tự bật/tắt loading toàn màn khi [showLoading] = true.
     * - Exception được chuyển tới [onError] nếu có, ngược lại tới [handleError].
     * - [CancellationException] luôn được ném lại (không coi là lỗi).
     *
     * @param context context bổ sung (thường không cần, xem quy ước coroutine ở trên)
     * @param showLoading có hiển thị loading toàn màn hay không
     * @param onError xử lý lỗi riêng cho tác vụ này
     */
    protected fun launch(
        context: CoroutineContext = EmptyCoroutineContext,
        showLoading: Boolean = true,
        onError: (suspend (Throwable) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit,
    ): Job = viewModelScope.launch(context) {
        if (showLoading) showLoading()
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (onError != null) onError(e) else handleError(e)
        } finally {
            if (showLoading) hideLoading()
        }
    }

    private val keyedJobs = ConcurrentHashMap<String, Job>()

    /**
     * Giống [launch] nhưng huỷ job trước đó có cùng [key].
     * Dùng cho search-as-you-type, refresh liên tục, chọn tab nhanh...
     *
     * ```kotlin
     * is SearchEvent.QueryChanged -> launchLatest("search", showLoading = false) {
     *     delay(300) // debounce
     *     setState { copy(results = repository.search(event.query)) }
     * }
     * ```
     */
    protected fun launchLatest(
        key: String,
        context: CoroutineContext = EmptyCoroutineContext,
        showLoading: Boolean = true,
        onError: (suspend (Throwable) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit,
    ): Job {
        keyedJobs[key]?.cancel()
        return launch(context, showLoading, onError, block).also { job ->
            keyedJobs[key] = job
            job.invokeOnCompletion { keyedJobs.remove(key, job) }
        }
    }

    /**
     * Collect một Flow (vd: từ Room / DataStore) trong [viewModelScope].
     * Lỗi được xử lý giống [launch]. Không hiển thị loading.
     *
     * ```kotlin
     * init {
     *     repository.observeUser().collectIn { user -> setState { copy(user = user) } }
     * }
     * ```
     */
    protected fun <T> Flow<T>.collectIn(
        onError: (suspend (Throwable) -> Unit)? = null,
        collector: suspend (T) -> Unit,
    ): Job = launch(showLoading = false, onError = onError) {
        collect { collector(it) }
    }
}
