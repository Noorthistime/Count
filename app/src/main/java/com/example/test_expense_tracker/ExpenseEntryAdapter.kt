package com.example.test_expense_tracker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.test_expense_tracker.data.ExpenseEntry
import com.example.test_expense_tracker.databinding.ItemExpenseEntryBinding
import java.util.*

class ExpenseEntryAdapter(
    private var entries: List<ExpenseEntry>,
    private val onEdit: (ExpenseEntry) -> Unit,
    private val onDelete: (ExpenseEntry) -> Unit
) : RecyclerView.Adapter<ExpenseEntryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemExpenseEntryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExpenseEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        holder.binding.tvItemNote.text = entry.note
        holder.binding.tvItemAmount.text = String.format(Locale.getDefault(), "₹%.2f", entry.amount)
        
        holder.binding.btnEdit.setOnClickListener { onEdit(entry) }
        holder.binding.btnDelete.setOnClickListener { onDelete(entry) }
    }

    override fun getItemCount() = entries.size

    fun updateData(newEntries: List<ExpenseEntry>) {
        this.entries = newEntries
        notifyDataSetChanged()
    }
}