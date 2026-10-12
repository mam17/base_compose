package com.example.basecompose.base

/**
 * Contract MVI (Model - View - Intent) dùng chung cho mọi màn hình.
 *
 * Mỗi màn định nghĩa 3 kiểu trong file `XxxContract.kt`:
 * - [UiState]  : toàn bộ dữ liệu để vẽ màn hình (data class, immutable).
 * - [UiEvent]  : hành động từ UI gửi lên ViewModel (click, nhập text, refresh...).
 * - [UiEffect] : sự kiện một lần từ ViewModel xuống UI (navigate, toast, mở dialog hệ thống...).
 *
 * Ví dụ:
 * ```kotlin
 * data class HomeState(
 *     val users: List<User> = emptyList(),
 *     val query: String = "",
 * ) : UiState
 *
 * sealed interface HomeEvent : UiEvent {
 *     data class QueryChanged(val value: String) : HomeEvent
 *     data class UserClicked(val id: String) : HomeEvent
 * }
 *
 * sealed interface HomeEffect : UiEffect {
 *     data class OpenDetail(val id: String) : HomeEffect
 * }
 * ```
 */
interface UiState

/** Hành động người dùng gửi từ UI lên ViewModel. Xem [UiState]. */
interface UiEvent

/** Sự kiện một lần (one-shot) gửi từ ViewModel xuống UI. Xem [UiState]. */
interface UiEffect

/** Dùng cho màn không có event / effect. */
data object NoEvent : UiEvent

/** Dùng cho màn không có event / effect. */
data object NoEffect : UiEffect
