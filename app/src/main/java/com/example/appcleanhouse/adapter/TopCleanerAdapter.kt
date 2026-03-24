package com.example.appcleanhouse.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appcleanhouse.R
import com.example.appcleanhouse.models.Cleaner

class TopCleanerAdapter(
    private val cleaners: MutableList<Cleaner>,
    private val onCleanerClick: (Cleaner) -> Unit
) : RecyclerView.Adapter<TopCleanerAdapter.TopCleanerViewHolder>() {

    inner class TopCleanerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.ivCleanerAvatar)
        val tvName: TextView = view.findViewById(R.id.tvCleanerName)
        val tvRating: TextView = view.findViewById(R.id.tvCleanerRating)
        val tvJobs: TextView = view.findViewById(R.id.tvCleanerJobs)
        val layoutTags: LinearLayout = view.findViewById(R.id.layoutTags)
        val btnBookNow: TextView = view.findViewById(R.id.btnBookNow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopCleanerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_top_cleaner, parent, false)
        return TopCleanerViewHolder(view)
    }

    override fun getItemCount(): Int = cleaners.size

    override fun onBindViewHolder(holder: TopCleanerViewHolder, position: Int) {
        val cleaner = cleaners[position]
        holder.ivAvatar.setImageResource(cleaner.avatarResId)
        holder.tvName.text = cleaner.name
        holder.tvRating.text = String.format("%.1f", cleaner.rating)
        holder.tvJobs.text = "(${cleaner.jobCount}+ jobs)"

        holder.layoutTags.removeAllViews()
        val chipText = mutableListOf<String>()
        chipText.addAll(cleaner.tags)
        if (chipText.none { it.contains("km", ignoreCase = true) }) {
            chipText.add(String.format("%.1f km", cleaner.distanceKm))
        }
        if (chipText.none { it.contains("$") || it.contains("/h") }) {
            chipText.add("$${cleaner.pricePerHour}/h")
        }

        chipText.take(3).forEach { tag ->
            val chip = TextView(holder.itemView.context).apply {
                text = tag
                textSize = 9f
                setTextColor(0xFF008080.toInt())
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(16, 6, 16, 6)
                setBackgroundResource(R.drawable.bg_circle_teal)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 8 }
            }
            holder.layoutTags.addView(chip)
        }

        holder.itemView.setOnClickListener { onCleanerClick(cleaner) }
        holder.btnBookNow.setOnClickListener { onCleanerClick(cleaner) }
    }

    fun submitData(newList: List<Cleaner>) {
        cleaners.clear()
        cleaners.addAll(newList)
        notifyDataSetChanged()
    }
}
