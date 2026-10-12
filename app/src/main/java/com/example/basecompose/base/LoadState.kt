package com.example.basecompose.base

/**
 * Trạng thái tải dữ liệu cho MỘT phần của màn hình (một list, một block...).
 *
 * Dùng làm field bên trong [UiState] khi màn có nhiều vùng load độc lập.
 * Loading toàn màn hình thì dùng `launch(showLoading = true)` của [BaseViewModel].
 *
 * Ví dụ:
 * ```kotlin
 * data class HomeState(
 *     val banners: LoadState<List<Banner>> = LoadState.Idle,
 *     val news: LoadState<List<News>> = LoadState.Idle,
 * ) : UiState
 *
 * // Trong ViewModel
 * launch(showLoading = false) {
 *     setState { copy(news = LoadState.Loading) }
 *     val result = loadState { repository.getNews() }
 *     setState { copy(news = result) }
 * }
 * ```
 */
sealed interface LoadState<out T> {

    /** Chưa thực hiện thao tác nào */
    data object Idle : LoadState<Nothing>

    /** Đang tải */
    data object Loading : LoadState<Nothing>

    /** Tải thành công */
    data class Success<T>(val data: T) : LoadState<T>

    /** Tải thất bại */
    data class Error(
        val message: UiText,
        val throwable: Throwable? = null,
    ) : LoadState<Nothing>

    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = (this as? Success)?.data
}

/**
 * Chạy [block] và gói kết quả thành [LoadState.Success] / [LoadState.Error].
 * CancellationException luôn được ném lại để không phá vỡ structured concurrency.
 */
suspend fun <T> loadState(block: suspend () -> T): LoadState<T> =
    try {
        LoadState.Success(block())
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        LoadState.Error(e.toUiText(), e)
    }
