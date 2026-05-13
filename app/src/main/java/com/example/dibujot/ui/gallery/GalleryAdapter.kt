package com.example.dibujot.ui.gallery

import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dibujot.R
import com.example.dibujot.data.DrawingItem

class GalleryAdapter(
    private val onClick: (DrawingItem) -> Unit
) : ListAdapter<DrawingItem, GalleryAdapter.ViewHolder>(DIFF_CALLBACK) {

    private var lastClickTime: Long = -500L

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_drawing, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position)) { item ->
            onItemClicked(item, SystemClock.elapsedRealtime())
        }
    }

    /**
     * Visible for testing: applies the double-tap guard and invokes [onClick] if allowed.
     */
    fun onItemClicked(item: DrawingItem, eventTime: Long) {
        if (eventTime - lastClickTime >= DOUBLE_TAP_GUARD_MS) {
            lastClickTime = eventTime
            onClick(item)
        }
    }

    /**
     * Visible for testing: triggers a simulated click on the item at [position]
     * bypassing the view hierarchy (useful when RecyclerView is not laid out in Robolectric).
     */
    fun simulateClick(position: Int) {
        onItemClicked(getItem(position), SystemClock.elapsedRealtime())
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.iv_drawing)
        private val nameView: TextView? = itemView.findViewById(R.id.tv_drawing_name)

        fun bind(item: DrawingItem, onClick: (DrawingItem) -> Unit) {
            imageView.setImageResource(item.imageResId)
            nameView?.text = item.name
            itemView.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private const val DOUBLE_TAP_GUARD_MS = 500L

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DrawingItem>() {
            override fun areItemsTheSame(oldItem: DrawingItem, newItem: DrawingItem): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: DrawingItem, newItem: DrawingItem): Boolean =
                oldItem == newItem
        }
    }
}
