package com.example.basecompose.utils.extensions

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Extension functions tiện ích cho View.
 */

/** Hiển thị View */
fun View.visible() {
    visibility = View.VISIBLE
}

/** Ẩn View (vẫn chiếm không gian) */
fun View.invisible() {
    visibility = View.INVISIBLE
}

/** Ẩn View (không chiếm không gian) */
fun View.gone() {
    visibility = View.GONE
}

/** Toggle visibility giữa VISIBLE và GONE */
fun View.toggleVisibility() {
    visibility = if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
}

/**
 * Set visibility dựa trên condition.
 * @param condition true → VISIBLE, false → GONE
 */
fun View.visibleIf(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
}

/**
 * Chống click liên tục (debounce click).
 * @param debounceTime Thời gian debounce tính bằng ms (mặc định 500ms)
 * @param action Action khi click
 */
fun View.setOnSafeClickListener(
    debounceTime: Long = 500L,
    action: (View) -> Unit
) {
    var lastClickTime = 0L
    setOnClickListener { view ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= debounceTime) {
            lastClickTime = currentTime
            action(view)
        }
    }
}

/**
 * Extension functions tiện ích cho Context.
 */

/** Hiển thị Toast ngắn */
fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

/** Hiển thị Toast dài */
fun Context.showLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

/**
 * Extension functions tiện ích cho Fragment.
 */

/** Hiển thị Toast từ Fragment */
fun Fragment.showToast(message: String) {
    context?.showToast(message)
}

/** Hiển thị Snackbar từ Fragment */
fun Fragment.showSnackbar(
    message: String,
    duration: Int = Snackbar.LENGTH_SHORT,
    actionText: String? = null,
    action: (() -> Unit)? = null
) {
    view?.let { rootView ->
        Snackbar.make(rootView, message, duration).apply {
            actionText?.let { text ->
                setAction(text) { action?.invoke() }
            }
        }.show()
    }
}

/**
 * Extension functions tiện ích cho Flow collection.
 */

/**
 * Collect Flow an toàn theo lifecycle.
 *
 * @param state Lifecycle.State tối thiểu để collect
 * @param collector Lambda xử lý giá trị
 */
fun <T> Flow<T>.collectIn(
    lifecycleOwner: LifecycleOwner,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    collector: suspend (T) -> Unit
) {
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(state) {
            collectLatest(collector)
        }
    }
}
