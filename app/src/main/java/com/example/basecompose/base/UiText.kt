package com.example.basecompose.base

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import com.example.basecompose.R
import java.io.IOException

/**
 * Text hiển thị cho UI mà ViewModel có thể tạo ra mà không cần [Context].
 *
 * ViewModel không nên giữ Context để lấy string resource, nên trả về [UiText]
 * và để Composable tự resolve (đúng ngôn ngữ, đúng configuration hiện tại).
 */
@Immutable
sealed interface UiText {

    /** Chuỗi động (vd: message từ server) */
    data class Dynamic(val value: String) : UiText

    /** Chuỗi từ string resource, hỗ trợ format args */
    data class Resource(
        @param:StringRes val id: Int,
        val args: List<Any> = emptyList(),
    ) : UiText

    @Composable
    fun asString(): String = when (this) {
        is Dynamic -> value
        is Resource -> stringResource(id, *args.toTypedArray())
    }

    fun asString(context: Context): String = when (this) {
        is Dynamic -> value
        is Resource -> context.getString(id, *args.toTypedArray())
    }

    companion object {
        operator fun invoke(value: String): UiText = Dynamic(value)
        operator fun invoke(@StringRes id: Int, vararg args: Any): UiText = Resource(id, args.toList())
    }
}

/**
 * Chuyển exception thành message hiển thị cho người dùng.
 * Mở rộng tại đây khi có thêm loại lỗi (HttpException, lỗi domain...).
 */
fun Throwable.toUiText(): UiText = when (this) {
    is IOException -> UiText(R.string.error_network)
    else -> message?.takeIf { it.isNotBlank() }?.let(UiText::Dynamic) ?: UiText(R.string.error_unknown)
}
