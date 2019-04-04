package com.v2d.trackme.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.v2d.trackme.R
import com.v2d.trackme.data.MyHistory
import kotlinx.android.synthetic.main.list_item_myhistory.view.*
import java.text.SimpleDateFormat


class MyHistoryAdapter(private val clickListener: (MyHistory) -> Unit) :
    ListAdapter<MyHistory, MyHistoryAdapter.ViewHolder>(MyHistoryDiffCallback()){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_myhistory, parent, false)
        )
    }

    override fun onBindViewHolder(holder: MyHistoryAdapter.ViewHolder, position: Int) {
        holder.bind(getItem(position), clickListener)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: MyHistory, clickListener: (MyHistory) -> Unit) {
            itemView.tvName.text = item.name
            itemView.tvDate.text = formatDate(item.createdDate.toString())
            itemView.setOnClickListener {
                clickListener(item)
            }
        }

        fun formatDate(date: String) : String {
            var format = SimpleDateFormat("EEE MMM dd hh:mm:ss Z yyyy")
            val newDate = format.parse(date)

            format = SimpleDateFormat("MM/dd/yyyy")
            return format.format(newDate)
        }
    }
}
private class MyHistoryDiffCallback : DiffUtil.ItemCallback<MyHistory>() {

    override fun areItemsTheSame(oldItem: MyHistory, newItem: MyHistory): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: MyHistory, newItem: MyHistory): Boolean {
        return oldItem == newItem
    }
}