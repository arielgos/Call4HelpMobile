package com.agos.call4help.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.agos.call4help.R
import com.agos.call4help.model.Event
import com.agos.call4help.toFormattedString

class EventAdapter(
    private val context: Context
) : ListAdapter<Event, EventAdapter.ViewHolder>(EventDiffCallback) {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val dateTextView: TextView = itemView.findViewById(R.id.date)
        private val detailTextView: TextView = itemView.findViewById(R.id.detail)
        private val userTextView: TextView = itemView.findViewById(R.id.user)
        private var current: Event? = null

        fun bind(item: Event) {
            current = item
            dateTextView.text = current?.date?.toFormattedString()
            detailTextView.text = current?.detail
            userTextView.text = current?.user
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.event_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

object EventDiffCallback : DiffUtil.ItemCallback<Event>() {
    override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
        return oldItem.date.time == newItem.date.time
    }
}
