package com.example.basecompose.base

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.example.basecompose.utils.network.LiveNetworkMonitor
import com.example.basecompose.utils.network.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Base Activity sử dụng ViewBinding generics.
 *
 * Tự động inflate ViewBinding, quản lý lifecycle, và cung cấp
 * các abstract methods theo pattern chuẩn.
 *
 * @param VB ViewBinding type của Activity
 *
 * Ví dụ sử dụng:
 * ```kotlin
 * @AndroidEntryPoint
 * class HomeActivity : BaseActivity<ActivityHomeBinding>(
 *     ActivityHomeBinding::inflate
 * ) {
 *     private val viewModel: HomeViewModel by viewModels()
 *
 *     override fun initView() {
 *         // Setup views, click listeners, etc.
 *         binding.btnSubmit.setOnClickListener { }
 *     }
 *
 *     override fun initData() {
 *         // Load initial data
 *         viewModel.loadData()
 *     }
 *
 *     override fun initObserver() {
 *         // Observe ViewModel states
 *         collectFlow(viewModel.user) { state ->
 *             when (state) {
 *                 is UiState.Loading -> showLoading()
 *                 is UiState.Success -> showData(state.data)
 *                 is UiState.Error -> showError(state.message)
 *                 else -> {}
 *             }
 *         }
 *     }
 * }
 * ```
 */
abstract class BaseActivity<VB : ViewBinding>(
    private val inflater: (LayoutInflater) -> VB
) : AppCompatActivity() {

    /** ViewBinding instance, accessible sau onCreate */
    protected lateinit var binding: VB
        private set

    /**
     * Bật / tắt tự động theo dõi và hiển thị DialogNetwork khi mất mạng.
     * Mặc định là true (tự động kiểm tra mạng toàn cục).
     * Override thành false nếu Activity này không yêu cầu kết nối mạng (ví dụ màn Offline).
     */
    open val autoCheckNetwork: Boolean = true

    /**
     * Instance giám sát trạng thái mạng.
     */
    protected val networkMonitor: NetworkMonitor by lazy {
        LiveNetworkMonitor(applicationContext)
    }

    private var networkDialog: DialogNetwork? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = inflater(layoutInflater)
        setContentView(binding.root)

        initView()
        initData()
        initObserver()

        if (autoCheckNetwork) {
            setupNetworkMonitoring()
        }
    }

    /**
     * Khởi tạo views: setup click listeners, adapters, toolbar, etc.
     * Được gọi sau khi ViewBinding đã inflate.
     */
    abstract fun initView()

    /**
     * Load dữ liệu ban đầu: gọi API, query database, etc.
     * Được gọi sau [initView].
     */
    abstract fun initData()

    /**
     * Đăng ký observers: observe LiveData, collect StateFlow, etc.
     * Được gọi sau [initData].
     */
    abstract fun initObserver()

    /**
     * Tiện ích collect StateFlow an toàn theo lifecycle.
     * Tự động cancel khi Activity ở trạng thái STOPPED.
     *
     * @param flow StateFlow cần collect
     * @param state Lifecycle.State tối thiểu để collect (mặc định STARTED)
     * @param collector Lambda xử lý giá trị emit
     */
    protected fun <T> collectFlow(
        flow: Flow<T>,
        state: Lifecycle.State = Lifecycle.State.STARTED,
        collector: suspend (T) -> Unit
    ) {
        lifecycleScope.launch {
            repeatOnLifecycle(state) {
                flow.collectLatest(collector)
            }
        }
    }

    // ==========================================
    // SYSTEM BARS & FULLSCREEN CONTROLLERS
    // ==========================================

    /**
     * Controller quản lý insets và các thanh hệ thống (Status Bar, Navigation Bar).
     */
    protected val windowInsetsController: WindowInsetsControllerCompat by lazy {
        WindowCompat.getInsetsController(window, window.decorView)
    }

    /**
     * Bật / tắt chế độ toàn màn hình (ẩn cả Status Bar và Navigation Bar).
     *
     * @param isFullScreen true để ẩn toàn bộ thanh hệ thống, false để hiện lại.
     * @param isSticky true để khi người dùng vuốt từ mép màn hình, thanh hệ thống chỉ hiện tạm thời rồi tự ẩn lại (Immersive Sticky).
     */
    fun setFullScreen(isFullScreen: Boolean = true, isSticky: Boolean = true) {
        if (isFullScreen) {
            windowInsetsController.systemBarsBehavior = if (isSticky) {
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            }
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    /**
     * Ẩn thanh điều hướng (Navigation Bar).
     *
     * @param isSticky true để tự ẩn lại sau khi người dùng vuốt chạm.
     */
    fun hideNavigationBar(isSticky: Boolean = true) {
        if (isSticky) {
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
    }

    /**
     * Hiển thị thanh điều hướng (Navigation Bar).
     */
    fun showNavigationBar() {
        windowInsetsController.show(WindowInsetsCompat.Type.navigationBars())
    }

    /**
     * Ẩn hoặc hiện thanh điều hướng tùy theo [isVisible].
     *
     * @param isVisible true để hiện, false để ẩn.
     * @param isSticky true để tự ẩn lại sau khi vuốt nếu đang ẩn.
     */
    fun setNavigationBarVisible(isVisible: Boolean, isSticky: Boolean = true) {
        if (isVisible) {
            showNavigationBar()
        } else {
            hideNavigationBar(isSticky)
        }
    }

    /**
     * Ẩn thanh trạng thái (Status Bar).
     *
     * @param isSticky true để tự ẩn lại sau khi người dùng vuốt chạm.
     */
    fun hideStatusBar(isSticky: Boolean = true) {
        if (isSticky) {
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
    }

    /**
     * Hiển thị thanh trạng thái (Status Bar).
     */
    fun showStatusBar() {
        windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
    }

    /**
     * Ẩn hoặc hiện thanh trạng thái tùy theo [isVisible].
     *
     * @param isVisible true để hiện, false để ẩn.
     * @param isSticky true để tự ẩn lại sau khi vuốt nếu đang ẩn.
     */
    fun setStatusBarVisible(isVisible: Boolean, isSticky: Boolean = true) {
        if (isVisible) {
            showStatusBar()
        } else {
            hideStatusBar(isSticky)
        }
    }

    /**
     * Đổi màu nền thanh trạng thái (Status Bar).
     *
     * @param color Mã màu cần đổi (@ColorInt).
     */
    @Suppress("DEPRECATION")
    fun setStatusBarColor(@ColorInt color: Int) {
        window.statusBarColor = color
    }

    /**
     * Đổi màu nền thanh trạng thái qua Resource ID.
     *
     * @param colorRes Resource ID của màu (R.color.xyz).
     */
    fun setStatusBarColorRes(@ColorRes colorRes: Int) {
        setStatusBarColor(ContextCompat.getColor(this, colorRes))
    }

    /**
     * Đổi màu nền thanh điều hướng (Navigation Bar).
     *
     * @param color Mã màu cần đổi (@ColorInt).
     */
    @Suppress("DEPRECATION")
    fun setNavigationBarColor(@ColorInt color: Int) {
        window.navigationBarColor = color
    }

    /**
     * Đổi màu nền thanh điều hướng qua Resource ID.
     *
     * @param colorRes Resource ID của màu (R.color.xyz).
     */
    fun setNavigationBarColorRes(@ColorRes colorRes: Int) {
        setNavigationBarColor(ContextCompat.getColor(this, colorRes))
    }

    /**
     * Đổi màu icon/chữ của thanh trạng thái (Status Bar).
     *
     * @param isDark true nếu muốn icon/chữ màu tối (phù hợp khi nền Status Bar màu sáng),
     *               false nếu muốn icon/chữ màu sáng (phù hợp khi nền Status Bar màu tối).
     */
    fun setStatusBarIconsColor(isDark: Boolean) {
        windowInsetsController.isAppearanceLightStatusBars = isDark
    }

    /**
     * Đổi màu icon thanh điều hướng (Navigation Bar).
     *
     * @param isDark true nếu muốn icon màu tối (phù hợp khi nền Navigation Bar màu sáng),
     *               false nếu muốn icon màu sáng (phù hợp khi nền Navigation Bar màu tối).
     */
    fun setNavigationBarIconsColor(isDark: Boolean) {
        windowInsetsController.isAppearanceLightNavigationBars = isDark
    }

    /**
     * Cấu hình nội dung layout có vừa khít các thanh hệ thống hay tràn viền (Edge-to-Edge).
     *
     * @param fitsSystemWindows true để nội dung tự co lại không bị che khuất bởi thanh hệ thống,
     *                          false để nội dung vẽ tràn xuống dưới các thanh hệ thống.
     */
    fun setDecorFitsSystemWindows(fitsSystemWindows: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(window, fitsSystemWindows)
    }

    // ==========================================
    // NETWORK MANAGEMENT (KIỂM TRA MẠNG TOÀN CỤC)
    // ==========================================

    /**
     * Thiết lập lắng nghe kết nối mạng tự động theo lifecycle của Activity.
     * Tự động hiển thị DialogNetwork khi mất mạng và tự động đóng khi có mạng lại.
     */
    private fun setupNetworkMonitoring() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                networkMonitor.isConnected.collectLatest { isConnected ->
                    if (!isConnected) {
                        showNetworkDialog()
                        onNetworkLost()
                    } else {
                        dismissNetworkDialog()
                        onNetworkAvailable()
                    }
                }
            }
        }
    }

    /**
     * Hiển thị Dialog thông báo mất mạng.
     *
     * @param onRetry Callback tùy chọn khi kết nối lại mạng thành công.
     */
    fun showNetworkDialog(onRetry: (() -> Unit)? = null) {
        if (networkDialog?.dialog?.isShowing == true) return
        networkDialog = DialogNetwork.show(supportFragmentManager, onRetry)
    }

    /**
     * Đóng Dialog thông báo mất mạng nếu đang hiển thị.
     */
    fun dismissNetworkDialog() {
        DialogNetwork.dismiss(supportFragmentManager)
        networkDialog = null
    }

    /**
     * Kiểm tra trạng thái mạng tức thời tại thời điểm gọi hàm.
     *
     * @return true nếu đang có Internet, false nếu không.
     */
    fun isNetworkConnected(): Boolean = networkMonitor.isCurrentlyConnected()

    /**
     * Hook được gọi khi thiết bị mất mạng.
     * Activity con có thể override để tạm dừng tác vụ (pause video, stop audio, hoãn sync...).
     */
    open fun onNetworkLost() {}

    /**
     * Hook được gọi khi thiết bị có mạng trở lại.
     * Activity con có thể override để tự động tải lại dữ liệu (refresh data).
     */
    open fun onNetworkAvailable() {}
}
