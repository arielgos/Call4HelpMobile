package com.agos.call4help.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.agos.call4help.R
import com.agos.call4help.Utils
import com.agos.call4help.model.Alert
import com.agos.call4help.toFormattedString
import com.bumptech.glide.Glide

class AlertAdapter(
    private val context: Context,
    private val onEditClickListener: OnClickListener,
    private val onImageClickListener: OnClickListener
) : ListAdapter<Alert, AlertAdapter.ViewHolder>(AlertDiffCallback) {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val imageView: ImageView = itemView.findViewById(R.id.image)
        private val dateTextView: TextView = itemView.findViewById(R.id.date)
        private val statusTextView: TextView = itemView.findViewById(R.id.status)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.description)
        private val tagsTextView: TextView = itemView.findViewById(R.id.tags)
        private val objectsTextView: TextView = itemView.findViewById(R.id.objects)
        private val editImageView: ImageView = itemView.findViewById(R.id.edit)

        private var current: Alert? = null

        fun bind(item: Alert, context: Context) {
            current = item
            if (current?.image?.isNotEmpty() == true) {
                Glide.with(context)
                    .asBitmap()
                    .load("${Utils.imageUrl}${current?.image}?alt=media")
                    .into(imageView)
            }
            dateTextView.text = current?.date?.toFormattedString()
            statusTextView.text = current?.status
            descriptionTextView.text = current?.description
            tagsTextView.text = current?.tags
            objectsTextView.text = current?.objects

            editImageView.visibility = View.GONE

            if (current?.status.toString().lowercase().contains("pendiente")) {
                statusTextView.setBackgroundResource(R.drawable.curve_pending)
                editImageView.visibility = View.VISIBLE
            }
            if (current?.status.toString().lowercase().contains("proceso")) {
                statusTextView.setBackgroundResource(R.drawable.curve_in_process)
            }
            if (current?.status.toString().lowercase().contains("cancelado")) {
                statusTextView.setBackgroundResource(R.drawable.curve_canceled)
            }
            if (current?.status.toString().lowercase().contains("solucionado")) {
                statusTextView.setBackgroundResource(R.drawable.curve_success)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.alert_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.findViewById<ImageView>(R.id.edit).setOnClickListener {
            onEditClickListener.onClick(item)
        }
        holder.itemView.findViewById<TextView>(R.id.date).setOnClickListener {
            onImageClickListener.onClick(item)
        }
        holder.itemView.findViewById<TextView>(R.id.description).setOnClickListener {
            onImageClickListener.onClick(item)
        }
        holder.itemView.findViewById<ImageView>(R.id.image).setOnClickListener {
            onImageClickListener.onClick(item)
        }
        holder.bind(item, context)
    }
}

object AlertDiffCallback : DiffUtil.ItemCallback<Alert>() {
    override fun areItemsTheSame(oldItem: Alert, newItem: Alert): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Alert, newItem: Alert): Boolean {
        return oldItem.id == newItem.id
    }
}

class OnClickListener(val clickListener: (item: Alert) -> Unit) {
    fun onClick(item: Alert) = clickListener(item)
}