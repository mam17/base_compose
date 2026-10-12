package com.example.basecompose.base

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.basecompose.R

/**
 * Base Compose Dialog - Các Composable functions tái sử dụng cho dialogs.
 *
 * Cung cấp 2 dạng dialog:
 * 1. [BaseComposeDialog] - Custom dialog với nội dung tùy ý
 * 2. [BaseAlertDialog] - AlertDialog chuẩn Material 3
 *
 * Ví dụ sử dụng BaseComposeDialog:
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     var showDialog by remember { mutableStateOf(false) }
 *
 *     if (showDialog) {
 *         BaseComposeDialog(
 *             onDismissRequest = { showDialog = false },
 *             dismissOnBackPress = true,
 *             dismissOnClickOutside = false
 *         ) {
 *             Column(modifier = Modifier.padding(24.dp)) {
 *                 Text("Custom Content")
 *                 Button(onClick = { showDialog = false }) {
 *                     Text("Close")
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * Ví dụ sử dụng BaseAlertDialog:
 * ```kotlin
 * @Composable
 * fun DeleteConfirmation() {
 *     var showDialog by remember { mutableStateOf(false) }
 *
 *     if (showDialog) {
 *         BaseAlertDialog(
 *             title = "Xác nhận xóa",
 *             message = "Bạn có chắc muốn xóa item này?",
 *             confirmText = "Xóa",
 *             dismissText = "Hủy",
 *             onConfirm = {
 *                 deleteItem()
 *                 showDialog = false
 *             },
 *             onDismiss = { showDialog = false }
 *         )
 *     }
 * }
 * ```
 */

/**
 * Custom Compose Dialog với nội dung tùy ý.
 *
 * Wrapper quanh [Dialog] với cấu hình phổ biến và Surface Material 3.
 *
 * @param onDismissRequest Callback khi dialog bị dismiss
 * @param dismissOnBackPress Cho phép dismiss khi nhấn back (mặc định true)
 * @param dismissOnClickOutside Cho phép dismiss khi tap ngoài (mặc định true)
 * @param usePlatformDefaultWidth Sử dụng width mặc định của platform (mặc định false)
 * @param content Nội dung Composable bên trong dialog
 */
@Composable
fun BaseComposeDialog(
    onDismissRequest: () -> Unit,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    usePlatformDefaultWidth: Boolean = false,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
            usePlatformDefaultWidth = usePlatformDefaultWidth
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            content()
        }
    }
}

/**
 * Alert Dialog chuẩn Material 3 với title, message, confirm/dismiss buttons.
 *
 * @param title Tiêu đề dialog
 * @param message Nội dung message
 * @param confirmText Text nút xác nhận (mặc định "Xác nhận")
 * @param dismissText Text nút hủy (mặc định "Hủy", null để ẩn)
 * @param onConfirm Callback khi nhấn xác nhận
 * @param onDismiss Callback khi dialog bị dismiss
 */
@Composable
fun BaseAlertDialog(
    title: String,
    message: String,
    confirmText: String = stringResource(R.string.common_confirm),
    dismissText: String? = stringResource(R.string.common_cancel),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = dismissText?.let {
            {
                TextButton(onClick = onDismiss) {
                    Text(it)
                }
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 6.dp
    )
}
