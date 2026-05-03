package com.example.test_expense_tracker

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.test_expense_tracker.data.ExpenseDao
import com.example.test_expense_tracker.data.ExpenseDatabase
import com.example.test_expense_tracker.databinding.ActivityDailyBreakdownBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DailyBreakdownActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDailyBreakdownBinding
    private lateinit var expenseDao: ExpenseDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDailyBreakdownBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        expenseDao = ExpenseDatabase.getDatabase(this).expenseDao()

        setupUI()
        loadData()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }
        
        val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
        binding.tvTitle.text = "DAILY BREAKDOWN ($monthName)"
    }

    private fun loadData() {
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()) + "%"
        lifecycleScope.launch {
            val monthlyEntries = expenseDao.getExpensesForMonth(monthKey)
            val dailyTotals = monthlyEntries.groupBy { it.date }
                .map { (date, entries) -> SummaryActivity.DailyTotal(date, entries.sumOf { it.amount }) }
                .sortedByDescending { it.date }
            
            binding.rvDailyExpenses.layoutManager = LinearLayoutManager(this@DailyBreakdownActivity)
            binding.rvDailyExpenses.adapter = DailyTotalAdapter(dailyTotals)
        }
    }
}