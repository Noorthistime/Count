package com.example.test_expense_tracker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.test_expense_tracker.databinding.ItemDailyExpenseBinding
import java.text.SimpleDateFormat
import java.util.*

class DailyTotalAdapter(private val dailyTotals: List<SummaryActivity.DailyTotal>) :
    RecyclerView.Adapter<DailyTotalAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemDailyExpenseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDailyExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dailyTotals[position]
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        
        try {
            val date = inputFormat.parse(item.date)
            holder.binding.tvItemDate.text = if (date != null) outputFormat.format(date) else item.date
        } catch (e: Exception) {
            holder.binding.tvItemDate.text = item.date
        }
        
        holder.binding.tvItemAmount.text = String.format(Locale.getDefault(), "₹%.2f", item.total)
    }

    override fun getItemCount() = dailyTotals.size
}