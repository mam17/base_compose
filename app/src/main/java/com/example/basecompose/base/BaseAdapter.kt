package com.example.basecompose.base

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * Base Adapter cho RecyclerView sử dụng ListAdapter với single view type.
 *
 * Tích hợp ViewBinding generics và DiffUtil cho hiệu suất cao.
 * Hỗ trợ click và long click callbacks.
 *
 * @param T Kiểu dữ liệu của item trong list
 * @param VB ViewBinding type của item layout
 * @param diffCallback DiffUtil.ItemCallback để so sánh items
 * @param inflater Lambda inflate ViewBinding
 *
 * Ví dụ sử dụng:
 * ```kotlin
 * class UserAdapter : BaseAdapter<User, ItemUserBinding>(
 *     diffCallback = object : DiffUtil.ItemCallback<User>() {
 *         override fun areItemsTheSame(oldItem: User, newItem: User) =
 *             oldItem.id == newItem.id
 *         override fun areContentsTheSame(oldItem: User, newItem: User) =
 *             oldItem == newItem
 *     },
 *     inflater = ItemUserBinding::inflate
 * ) {
 *     override fun onBind(binding: ItemUserBinding, item: User, position: Int) {
 *         binding.tvName.text = item.name
 *         binding.tvEmail.text = item.email
 *         binding.ivAvatar.load(item.avatarUrl)
 *     }
 * }
 *
 * // Sử dụng trong Activity/Fragment:
 * val adapter = UserAdapter().apply {
 *     onItemClick = { user, position -> navigateToDetail(user) }
 *     onItemLongClick = { user, position -> showDeleteDialog(user) }
 * }
 * binding.recyclerView.adapter = adapter
 * adapter.submitList(userList)
 * ```
 */
abstract class BaseAdapter<T : Any, VB : ViewBinding>(
    diffCallback: DiffUtil.ItemCallback<T>,
    private val inflater: (LayoutInflater, ViewGroup?, Boolean) -> VB
) : ListAdapter<T, BaseAdapter.BaseViewHolder<VB>>(diffCallback) {

    /** Callback khi item được click */
    var onItemClick: ((item: T, position: Int) -> Unit)? = null

    /** Callback khi item được long click */
    var onItemLongClick: ((item: T, position: Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<VB> {
        val binding = inflater(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BaseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<VB>, position: Int) {
        val item = getItem(position)

        onBind(holder.binding, item, position)

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item, holder.bindingAdapterPosition)
        }
        holder.itemView.setOnLongClickListener {
            onItemLongClick?.invoke(item, holder.bindingAdapterPosition)
            onItemLongClick != null
        }
    }

    /**
     * Bind dữ liệu vào ViewBinding.
     * Override method này trong subclass để setup views.
     *
     * @param binding ViewBinding instance của item
     * @param item Dữ liệu của item tại vị trí hiện tại
     * @param position Vị trí của item trong list
     */
    abstract fun onBind(binding: VB, item: T, position: Int)

    /** ViewHolder chứa ViewBinding instance */
    class BaseViewHolder<VB : ViewBinding>(
        val binding: VB
    ) : RecyclerView.ViewHolder(binding.root)
}
