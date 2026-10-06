package com.example.basecompose.base

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * Base Multi Adapter cho RecyclerView hỗ trợ nhiều view types.
 *
 * Sử dụng ListAdapter với DiffUtil và ViewBinding.
 * Mỗi view type được đại diện bởi một [ItemViewType].
 *
 * @param T Kiểu dữ liệu chung (thường là sealed class hoặc interface)
 * @param diffCallback DiffUtil.ItemCallback để so sánh items
 *
 * Ví dụ sử dụng:
 * ```kotlin
 * // 1. Định nghĩa sealed class cho các loại item
 * sealed class FeedItem {
 *     data class Header(val title: String) : FeedItem()
 *     data class Post(val id: Int, val content: String) : FeedItem()
 *     data class Ad(val imageUrl: String) : FeedItem()
 * }
 *
 * // 2. Tạo adapter
 * class FeedAdapter : BaseMultiAdapter<FeedItem>(
 *     diffCallback = object : DiffUtil.ItemCallback<FeedItem>() {
 *         override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
 *             return when {
 *                 oldItem is FeedItem.Header && newItem is FeedItem.Header ->
 *                     oldItem.title == newItem.title
 *                 oldItem is FeedItem.Post && newItem is FeedItem.Post ->
 *                     oldItem.id == newItem.id
 *                 else -> false
 *             }
 *         }
 *         override fun areContentsTheSame(oldItem: FeedItem, newItem: FeedItem) =
 *             oldItem == newItem
 *     }
 * ) {
 *     companion object {
 *         const val TYPE_HEADER = 0
 *         const val TYPE_POST = 1
 *         const val TYPE_AD = 2
 *     }
 *
 *     override fun getItemViewType(position: Int): Int {
 *         return when (getItem(position)) {
 *             is FeedItem.Header -> TYPE_HEADER
 *             is FeedItem.Post -> TYPE_POST
 *             is FeedItem.Ad -> TYPE_AD
 *         }
 *     }
 *
 *     override fun getItemViewTypes(): List<ItemViewType<FeedItem, *>> = listOf(
 *         ItemViewType(
 *             viewType = TYPE_HEADER,
 *             inflater = ItemHeaderBinding::inflate,
 *             binder = { binding, item, _ ->
 *                 binding as ItemHeaderBinding
 *                 (item as FeedItem.Header).let {
 *                     binding.tvTitle.text = it.title
 *                 }
 *             }
 *         ),
 *         ItemViewType(
 *             viewType = TYPE_POST,
 *             inflater = ItemPostBinding::inflate,
 *             binder = { binding, item, _ ->
 *                 binding as ItemPostBinding
 *                 (item as FeedItem.Post).let {
 *                     binding.tvContent.text = it.content
 *                 }
 *             }
 *         ),
 *         ItemViewType(
 *             viewType = TYPE_AD,
 *             inflater = ItemAdBinding::inflate,
 *             binder = { binding, item, _ ->
 *                 binding as ItemAdBinding
 *                 (item as FeedItem.Ad).let {
 *                     binding.ivAd.load(it.imageUrl)
 *                 }
 *             }
 *         )
 *     )
 * }
 *
 * // 3. Sử dụng
 * val adapter = FeedAdapter().apply {
 *     onItemClick = { item, position ->
 *         when (item) {
 *             is FeedItem.Post -> navigateToPost(item)
 *             else -> {}
 *         }
 *     }
 * }
 * adapter.submitList(feedItems)
 * ```
 */
abstract class BaseMultiAdapter<T : Any>(
    diffCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, BaseMultiAdapter.MultiViewHolder>(diffCallback) {

    /** Callback khi item được click */
    var onItemClick: ((item: T, position: Int) -> Unit)? = null

    /** Callback khi item được long click */
    var onItemLongClick: ((item: T, position: Int) -> Unit)? = null

    /** Cache danh sách view types để tránh tạo lại mỗi lần */
    private val itemViewTypes: Map<Int, ItemViewType<T, *>> by lazy {
        getItemViewTypes().associateBy { it.viewType }
    }

    /**
     * Trả về danh sách các [ItemViewType] được hỗ trợ.
     * Mỗi [ItemViewType] chứa thông tin inflate và bind cho một view type cụ thể.
     */
    abstract fun getItemViewTypes(): List<ItemViewType<T, *>>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultiViewHolder {
        val itemViewType = itemViewTypes[viewType]
            ?: throw IllegalArgumentException(
                "View type $viewType chưa được đăng ký trong getItemViewTypes(). " +
                        "Đảm bảo getItemViewType() trả về view type đã được khai báo."
            )

        val binding = itemViewType.inflater(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MultiViewHolder(binding)
    }

    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(holder: MultiViewHolder, position: Int) {
        val item = getItem(position)
        val viewType = getItemViewType(position)
        val itemViewType = itemViewTypes[viewType] as? ItemViewType<T, ViewBinding>
            ?: return

        itemViewType.binder(holder.binding, item, position)

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item, holder.bindingAdapterPosition)
        }
        holder.itemView.setOnLongClickListener {
            onItemLongClick?.invoke(item, holder.bindingAdapterPosition)
            onItemLongClick != null
        }
    }

    /**
     * Cấu hình cho mỗi view type.
     *
     * @param T Kiểu dữ liệu của item
     * @param VB ViewBinding type cho view type này
     * @param viewType Int identifier cho view type (phải unique)
     * @param inflater Lambda inflate ViewBinding
     * @param binder Lambda bind dữ liệu vào ViewBinding
     */
    data class ItemViewType<T, VB : ViewBinding>(
        val viewType: Int,
        val inflater: (LayoutInflater, ViewGroup?, Boolean) -> VB,
        val binder: (binding: ViewBinding, item: T, position: Int) -> Unit
    )

    /** ViewHolder chung cho tất cả view types */
    class MultiViewHolder(
        val binding: ViewBinding
    ) : RecyclerView.ViewHolder(binding.root)
}
