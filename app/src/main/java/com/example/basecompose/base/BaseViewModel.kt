package com.example.basecompose.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel cung cấp các tiện ích chung cho tất cả ViewModel trong ứng dụng.
 *
 * Features:
 * - Loading state management tự động
 * - Error handling với CoroutineExceptionHandler
 * - Safe launch coroutines với xử lý exception
 * - UiState flow cho UI observe
 *
 * Ví dụ sử dụng:
 * ```kotlin
 * @HiltViewModel
 * class HomeViewModel @Inject constructor(
 *     private val getUserUseCase: GetUserUseCase
 * ) : BaseViewModel() {
 *
 *     private val _user = MutableStateFlow<UiState<User>>(UiState.Idle)
 *     val user: StateFlow<UiState<User>> = _user.asStateFlow()
 *
 *     fun loadUser(id: String) {
 *         safeLaunch {
 *             _user.value = UiState.Loading
 *             val result = getUserUseCase(id)
 *             _user.value = UiState.Success(result)
 *         }
 *     }
 * }
 * ```
 */
abstract class BaseViewModel : ViewModel() {

    /** Trạng thái loading chung, có thể observe từ UI */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Trạng thái error chung, có thể observe từ UI */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * CoroutineExceptionHandler mặc định.
     * Override để custom xử lý exception trong subclass.
     */
    protected open val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        handleException(throwable)
    }

    /**
     * Launch coroutine an toàn với exception handling tự động.
     *
     * @param dispatcher CoroutineDispatcher (mặc định IO)
     * @param showLoading Tự động set loading state (mặc định true)
     * @param onError Callback khi có lỗi (optional, mặc định dùng [handleException])
     * @param block Suspend function cần thực thi
     * @return Job của coroutine
     */
    protected fun safeLaunch(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        showLoading: Boolean = true,
        onError: ((Throwable) -> Unit)? = null,
        block: suspend () -> Unit
    ): Job {
        return viewModelScope.launch(dispatcher + exceptionHandler) {
            try {
                if (showLoading) _isLoading.value = true
                block()
            } catch (e: Exception) {
                onError?.invoke(e) ?: handleException(e)
            } finally {
                if (showLoading) _isLoading.value = false
            }
        }
    }

    /**
     * Xử lý exception mặc định.
     * Override để custom xử lý trong subclass.
     */
    protected open fun handleException(throwable: Throwable) {
        _error.value = throwable.message ?: "Đã xảy ra lỗi không xác định"
    }

    /** Xóa trạng thái error */
    fun clearError() {
        _error.value = null
    }

    /** Hiển thị loading */
    protected fun showLoading() {
        _isLoading.value = true
    }

    /** Ẩn loading */
    protected fun hideLoading() {
        _isLoading.value = false
    }
}
