package com.example.basecompose.base

import android.content.Intent
import android.provider.Settings
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.basecompose.databinding.DialogNetworkBinding
import com.example.basecompose.utils.extensions.showToast
import com.example.basecompose.utils.network.LiveNetworkMonitor
import com.example.basecompose.utils.network.NetworkMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * DialogNetwork - Dialog thông báo mất kết nối Internet toàn cục.
 *
 * Tính năng:
 * - Kế thừa BaseDialog sử dụng ViewBinding.
 * - Không thể bấm ra ngoài hoặc nút Back để tắt dialog khi chưa có mạng.
 * - Tự động phát hiện khi mạng được bật lại để tự đóng dialog (auto-dismiss).
 * - Nút "Thử lại": Kiểm tra mạng tức thì, nếu có mạng sẽ đóng dialog và gọi callback [onRetry].
 * - Nút "Mở cài đặt mạng": Mở trang cấu hình mạng của thiết bị.
 */
class DialogNetwork : BaseDialog<DialogNetworkBinding>(DialogNetworkBinding::inflate) {

    /** Callback tùy chọn được gọi khi người dùng bấm Thử lại và đã kết nối lại mạng thành công */
    var onRetry: (() -> Unit)? = null

    private val networkMonitor: NetworkMonitor by lazy {
        LiveNetworkMonitor(requireContext().applicationContext)
    }

    override fun isCancelableDialog(): Boolean = false

    override fun getDialogWidthRatio(): Float = 0.88f

    override fun initView() {
        binding.btnRetry.setOnClickListener {
            handleRetry()
        }

        binding.btnSettings.setOnClickListener {
            openNetworkSettings()
        }
    }

    override fun initData() {
        observeNetworkState()
    }

    /**
     * Tự động đóng dialog khi phát hiện thiết bị đã kết nối mạng trở lại.
     */
    private fun observeNetworkState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                networkMonitor.isConnected.collectLatest { isConnected ->
                    if (isConnected && isAdded) {
                        dismissAllowingStateLoss()
                        onRetry?.invoke()
                    }
                }
            }
        }
    }

    /**
     * Xử lý khi người dùng ấn nút "Thử lại".
     */
    private fun handleRetry() {
        binding.progressBar.isVisible = true
        binding.btnRetry.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            // Delay nhẹ để có hiệu ứng kiểm tra mạng
            delay(600)

            if (!isAdded) return@launch

            val isConnected = networkMonitor.isCurrentlyConnected()
            binding.progressBar.isVisible = false
            binding.btnRetry.isEnabled = true

            if (isConnected) {
                dismissAllowingStateLoss()
                onRetry?.invoke()
            } else {
                context?.showToast("Vẫn chưa có kết nối Internet. Vui lòng kiểm tra lại!")
            }
        }
    }

    /**
     * Mở cài đặt mạng trên thiết bị Android.
     */
    private fun openNetworkSettings() {
        try {
            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
            startActivity(intent)
        } catch (_: Exception) {
            try {
                val wifiIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
                startActivity(wifiIntent)
            } catch (_: Exception) {
                context?.showToast("Không thể mở màn hình cài đặt mạng.")
            }
        }
    }

    companion object {
        const val TAG = "DialogNetwork"

        /**
         * Tạo mới hoặc lấy instance của DialogNetwork.
         */
        fun newInstance(onRetry: (() -> Unit)? = null): DialogNetwork {
            return DialogNetwork().apply {
                this.onRetry = onRetry
            }
        }

        /**
         * Hiển thị dialog an toàn, tránh mở nhiều dialog đè lên nhau.
         */
        fun show(fragmentManager: FragmentManager, onRetry: (() -> Unit)? = null): DialogNetwork? {
            if (fragmentManager.isStateSaved || fragmentManager.isDestroyed) {
                return null
            }

            val existingDialog = fragmentManager.findFragmentByTag(TAG) as? DialogNetwork
            if (existingDialog != null && existingDialog.dialog?.isShowing == true) {
                return existingDialog
            }

            val dialog = newInstance(onRetry)
            dialog.show(fragmentManager, TAG)
            return dialog
        }

        /**
         * Đóng dialog nếu đang hiển thị.
         */
        fun dismiss(fragmentManager: FragmentManager) {
            val existingDialog = fragmentManager.findFragmentByTag(TAG) as? DialogNetwork
            existingDialog?.dismissAllowingStateLoss()
        }
    }
}
