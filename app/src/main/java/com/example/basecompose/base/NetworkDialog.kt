package com.example.basecompose.base

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.basecompose.R
import com.example.basecompose.ui.theme.BaseComposeTheme

import androidx.compose.foundation.layout.height
import com.example.basecompose.ui.theme.dimens

/**
 * Dialog thông báo mất kết nối Internet (bản Compose của DialogNetwork).
 *
 * Không thể đóng bằng Back hay chạm ra ngoài; tự biến mất khi [BaseApp] phát hiện có mạng lại.
 */
@Composable
fun NetworkDialog(
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    BaseComposeDialog(
        onDismissRequest = {},
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.dimens.dialogPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.itemSpacing),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(MaterialTheme.dimens.space72),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_wifi_off),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(MaterialTheme.dimens.space40),
                    )
                }
            }
            Text(
                text = stringResource(R.string.network_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.network_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MaterialTheme.dimens.buttonHeight),
            ) {
                Text(stringResource(R.string.network_retry))
            }
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MaterialTheme.dimens.buttonHeight),
            ) {
                Text(stringResource(R.string.network_settings))
            }
        }
    }
}

@Preview
@Composable
private fun NetworkDialogPreview() {
    BaseComposeTheme { NetworkDialog(onRetry = {}, onOpenSettings = {}) }
}
