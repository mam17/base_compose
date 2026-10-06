package com.example.basecompose.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Base Fragment sử dụng ViewBinding generics.
 *
 * Tự động inflate/destroy ViewBinding theo lifecycle của Fragment,
 * tránh memory leak khi Fragment view bị destroy nhưng Fragment vẫn alive.
 *
 * @param VB ViewBinding type của Fragment
 *
 * Ví dụ sử dụng:
 * ```kotlin
 * @AndroidEntryPoint
 * class HomeFragment : BaseFragment<FragmentHomeBinding>(
 *     FragmentHomeBinding::inflate
 * ) {
 *     private val viewModel: HomeViewModel by viewModels()
 *     // Hoặc share ViewModel với Activity:
 *     // private val sharedViewModel: MainViewModel by activityViewModels()
 *
 *     override fun initView() {
 *         binding.recyclerView.adapter = myAdapter
 *         binding.btnRefresh.setOnClickListener { viewModel.refresh() }
 *     }
 *
 *     override fun initData() {
 *         viewModel.loadData()
 *     }
 *
 *     override fun initObserver() {
 *         collectFlow(viewModel.items) { state ->
 *             when (state) {
 *                 is UiState.Success -> myAdapter.submitList(state.data)
 *                 is UiState.Error -> showError(state.message)
 *                 else -> {}
 *             }
 *         }
 *     }
 * }
 * ```
 */
abstract class BaseFragment<VB : ViewBinding>(
    private val inflater: (LayoutInflater, ViewGroup?, Boolean) -> VB
) : Fragment() {

    /**
     * ViewBinding instance.
     * Chỉ truy cập được giữa [onCreateView] và [onDestroyView].
     * Sử dụng [binding] property thay vì truy cập trực tiếp.
     */
    private var _binding: VB? = null

    /** ViewBinding instance an toàn, throw exception nếu truy cập ngoài lifecycle */
    protected val binding: VB
        get() = _binding ?: throw IllegalStateException(
            "ViewBinding is only valid between onCreateView and onDestroyView. " +
                    "Đảm bảo không truy cập binding trong coroutine sau khi view bị destroy."
        )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = this.inflater(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initData()
        initObserver()
    }

    /**
     * Khởi tạo views: setup click listeners, adapters, toolbar, etc.
     * Được gọi trong [onViewCreated] sau khi ViewBinding đã inflate.
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
     * Tiện ích collect StateFlow an toàn theo lifecycle của view.
     * Sử dụng [viewLifecycleOwner] thay vì Fragment lifecycle
     * để tránh collect khi view đã destroy.
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
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(state) {
                flow.collectLatest(collector)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
