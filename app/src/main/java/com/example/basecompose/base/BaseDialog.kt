package com.example.basecompose.base

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding

/**
 * Base DialogFragment sử dụng ViewBinding generics.
 *
 * Cung cấp các cấu hình phổ biến cho Dialog:
 * - Kích thước dialog (width/height)
 * - Background transparent
 * - Dim amount
 * - Cancelable behavior
 *
 * @param VB ViewBinding type của dialog layout
 *
 * Ví dụ sử dụng:
 * ```kotlin
 * class ConfirmDialog : BaseDialog<DialogConfirmBinding>(
 *     DialogConfirmBinding::inflate
 * ) {
 *     // Custom dialog width (mặc định 85% màn hình)
 *     override fun getDialogWidthRatio(): Float = 0.9f
 *
 *     // Không cho dismiss khi tap bên ngoài
 *     override fun isCancelableDialog(): Boolean = false
 *
 *     var onConfirm: (() -> Unit)? = null
 *     var onCancel: (() -> Unit)? = null
 *
 *     override fun initView() {
 *         binding.tvTitle.text = arguments?.getString("title") ?: "Xác nhận"
 *         binding.tvMessage.text = arguments?.getString("message") ?: ""
 *
 *         binding.btnConfirm.setOnClickListener {
 *             onConfirm?.invoke()
 *             dismiss()
 *         }
 *         binding.btnCancel.setOnClickListener {
 *             onCancel?.invoke()
 *             dismiss()
 *         }
 *     }
 *
 *     override fun initData() { }
 *
 *     companion object {
 *         fun newInstance(title: String, message: String) = ConfirmDialog().apply {
 *             arguments = Bundle().apply {
 *                 putString("title", title)
 *                 putString("message", message)
 *             }
 *         }
 *     }
 * }
 *
 * // Hiển thị dialog
 * ConfirmDialog.newInstance("Xóa?", "Bạn có chắc muốn xóa?").apply {
 *     onConfirm = { deleteItem() }
 * }.show(supportFragmentManager, "confirm")
 * ```
 */
abstract class BaseDialog<VB : ViewBinding>(
    private val inflater: (LayoutInflater, ViewGroup?, Boolean) -> VB
) : DialogFragment() {

    private var _binding: VB? = null

    /** ViewBinding instance an toàn */
    protected val binding: VB
        get() = _binding ?: throw IllegalStateException(
            "ViewBinding chỉ hợp lệ giữa onCreateView và onDestroyView."
        )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCancelable(isCancelableDialog())
            setCanceledOnTouchOutside(isCancelableDialog())
        }
    }

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
    }

    override fun onStart() {
        super.onStart()
        setupDialogWindow()
    }

    /**
     * Cấu hình window của Dialog (size, dim, animations).
     * Override để custom hoàn toàn.
     */
    protected open fun setupDialogWindow() {
        dialog?.window?.apply {
            val displayMetrics = resources.displayMetrics
            val width = (displayMetrics.widthPixels * getDialogWidthRatio()).toInt()

            setLayout(width, getDialogHeight())
            setDimAmount(getDimAmount())

            // Apply custom animation nếu có
            getDialogAnimationStyle()?.let {
                setWindowAnimations(it)
            }
        }
    }

    /**
     * Khởi tạo views: setup click listeners, hiển thị dữ liệu, etc.
     */
    abstract fun initView()

    /**
     * Load dữ liệu cho dialog (optional).
     */
    abstract fun initData()

    /**
     * Tỷ lệ chiều rộng dialog so với màn hình (0.0 → 1.0).
     * Mặc định: 85% chiều rộng màn hình.
     */
    protected open fun getDialogWidthRatio(): Float = 0.85f

    /**
     * Chiều cao dialog.
     * Mặc định: WRAP_CONTENT.
     * Override để set chiều cao cụ thể hoặc MATCH_PARENT.
     */
    protected open fun getDialogHeight(): Int = WindowManager.LayoutParams.WRAP_CONTENT

    /**
     * Độ tối của background phía sau dialog (0.0 → 1.0).
     * Mặc định: 0.5 (50% dim).
     */
    protected open fun getDimAmount(): Float = 0.5f

    /**
     * Cho phép dismiss dialog khi tap bên ngoài hoặc nhấn back.
     * Mặc định: true.
     */
    protected open fun isCancelableDialog(): Boolean = true

    /**
     * Animation style resource cho dialog (enter/exit animations).
     * Mặc định: null (không animation custom).
     *
     * Ví dụ: return R.style.DialogSlideAnimation
     */
    protected open fun getDialogAnimationStyle(): Int? = null

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
