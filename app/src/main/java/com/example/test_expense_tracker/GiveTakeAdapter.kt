package com.example.test_expense_tracker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.test_expense_tracker.data.GiveTakeEntry
import com.example.test_expense_tracker.databinding.ItemGiveTakeBinding
import java.util.*

class GiveTakeAdapter(
    private var entries: List<GiveTakeEntry>,
    private val onEdit: (GiveTakeEntry) -> Unit,
    private val onDelete: (GiveTakeEntry) -> Unit
) : RecyclerView.Adapter<GiveTakeAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemGiveTakeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGiveTakeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        holder.binding.tvPersonName.text = entry.personName
        holder.binding.tvReason.text = entry.reason
        holder.binding.tvDate.text = entry.date
        holder.binding.tvAmount.text = String.format(Locale.getDefault(), "₹%.2f", entry.amount)
        
        holder.binding.btnEdit.setOnClickListener { onEdit(entry) }
        holder.binding.btnDelete.setOnClickListener { onDelete(entry) }
    }

    override fun getItemCount() = entries.size

    fun updateData(newEntries: List<GiveTakeEntry>) {
        this.entries = newEntries
        notifyDataSetChanged()
    }
}