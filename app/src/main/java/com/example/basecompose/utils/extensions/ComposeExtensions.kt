package com.example.basecompose.utils.extensions

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.semantics.Role

/**
 * Trả về lambda chống click liên tục (bản Compose của setOnSafeClickListener).
 *
 * ```kotlin
 * Button(onClick = rememberDebouncedClick { onEvent(LoginEvent.Submit) }) { ... }
 * ```
 */
@Composable
fun rememberDebouncedClick(
    debounceMillis: Long = 500L,
    onClick: () -> Unit,
): () -> Unit {
    val currentOnClick by rememberUpdatedState(onClick)
    var lastClick by remember { mutableLongStateOf(0L) }
    return remember(debounceMillis) {
        {
            val now = System.currentTimeMillis()
            if (now - lastClick >= debounceMillis) {
                lastClick = now
                currentOnClick()
            }
        }
    }
}

/** `Modifier.clickable` có chống click liên tục. */
fun Modifier.debouncedClickable(
    enabled: Boolean = true,
    role: Role? = null,
    debounceMillis: Long = 500L,
    onClick: () -> Unit,
): Modifier = composed {
    val debounced = rememberDebouncedClick(debounceMillis, onClick)
    clickable(enabled = enabled, role = role, onClick = debounced)
}
