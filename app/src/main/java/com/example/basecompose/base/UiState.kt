package com.example.basecompose.base

/**
 * Sealed class đại diện cho các trạng thái UI phổ biến.
 *
 * Sử dụng trong ViewModel để emit trạng thái và trong View để observe.
 *
 * @param T Kiểu dữ liệu trả về khi thành công
 *
 * Ví dụ sử dụng:
 * ```kotlin
 * // Trong ViewModel
 * private val _userState = MutableStateFlow<UiState<User>>(UiState.Idle)
 * val userState: StateFlow<UiState<User>> = _userState.asStateFlow()
 *
 * fun loadUser() {
 *     _userState.value = UiState.Loading
 *     viewModelScope.launch {
 *         try {
 *             val user = repository.getUser()
 *             _userState.value = UiState.Success(user)
 *         } catch (e: Exception) {
 *             _userState.value = UiState.Error(e.message)
 *         }
 *     }
 * }
 *
 * // Trong Activity/Fragment
 * viewModel.userState.collect { state ->
 *     when (state) {
 *         is UiState.Idle -> { }
 *         is UiState.Loading -> showLoading()
 *         is UiState.Success -> showData(state.data)
 *         is UiState.Error -> showError(state.message)
 *     }
 * }
 * ```
 */
sealed class UiState<out T> {

    /** Trạng thái mặc định, chưa có action nào được thực hiện */
    data object Idle : UiState<Nothing>()

    /** Đang loading dữ liệu */
    data object Loading : UiState<Nothing>()

    /**
     * Load dữ liệu thành công
     * @param data Dữ liệu trả về
     */
    data class Success<T>(val data: T) : UiState<T>()

    /**
     * Có lỗi xảy ra
     * @param message Thông báo lỗi
     * @param throwable Exception gốc (optional)
     */
    data class Error(
        val message: String? = null,
        val throwable: Throwable? = null
    ) : UiState<Nothing>()
}
