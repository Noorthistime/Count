package com.example.test_expense_tracker

import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.test_expense_tracker.data.ExpenseDao
import com.example.test_expense_tracker.data.ExpenseDatabase
import com.example.test_expense_tracker.databinding.ActivitySummaryBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SummaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySummaryBinding
    private lateinit var expenseDao: ExpenseDao
    private var weeklyCalendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        expenseDao = ExpenseDatabase.getDatabase(this).expenseDao()

        setupUI()
        setupCharts()
        loadAllData()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }
        
        binding.btnPrevWeek.setOnClickListener {
            weeklyCalendar.add(Calendar.WEEK_OF_YEAR, -1)
            loadWeeklyData()
        }
        
        binding.btnNextWeek.setOnClickListener {
            weeklyCalendar.add(Calendar.WEEK_OF_YEAR, 1)
            loadWeeklyData()
        }

        binding.btnDailyBreakdown.setOnClickListener {
            startActivity(android.content.Intent(this, DailyBreakdownActivity::class.java))
        }

        val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
        binding.btnDailyBreakdown.text = "DAILY BREAKDOWN ($monthName)"
    }

    private fun setupCharts() {
        val charts = listOf(binding.weeklyBarChart, binding.monthlySplitChart, binding.yearlyLineChart)
        charts.forEach { chart ->
            chart.description.isEnabled = false
            chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
            chart.xAxis.textColor = Color.WHITE
            chart.xAxis.setDrawGridLines(false)
            chart.axisLeft.textColor = Color.WHITE
            chart.axisRight.isEnabled = false
            chart.legend.isEnabled = false
            chart.setNoDataTextColor(Color.GRAY)
            chart.setTouchEnabled(true)
        }
        
        // Enable horizontal scrolling for monthly
        binding.monthlySplitChart.isDragEnabled = true
        binding.monthlySplitChart.setScaleEnabled(false)
        binding.monthlySplitChart.setPinchZoom(false)
    }

    private fun loadAllData() {
        val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
        binding.tvWeeklyTitle.text = "WEEKLY OVERVIEW ($monthName)"
        binding.tvMonthlyTitle.text = "MONTHLY OVERVIEW ($monthName)"
        
        loadWeeklyData()
        loadMonthlyData()
        loadYearlyData()
    }

    private fun loadWeeklyData() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd", Locale.getDefault())
        val cal = weeklyCalendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        
        val weekEntries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        
        lifecycleScope.launch {
            for (i in 0..6) {
                val dateStr = sdf.format(cal.time)
                val entries = expenseDao.getExpensesByDate(dateStr)
                val total = entries.sumOf { it.amount }
                weekEntries.add(BarEntry(i.toFloat(), total.toFloat()))
                
                // Brief Labels like "Mon-01" in a single line
                labels.add("${dayFormat.format(cal.time)}-${dateFormat.format(cal.time)}")
                
                cal.add(Calendar.DATE, 1)
            }
            
            val dataSet = BarDataSet(weekEntries, "").apply {
                color = Color.parseColor("#FF0000")
                valueTextColor = Color.WHITE
                valueTextSize = 10f
            }
            binding.weeklyBarChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            binding.weeklyBarChart.xAxis.granularity = 1f
            binding.weeklyBarChart.xAxis.labelCount = 7
            binding.weeklyBarChart.xAxis.labelRotationAngle = 0f
            binding.weeklyBarChart.data = BarData(dataSet)
            binding.weeklyBarChart.invalidate()
        }
    }

    private fun loadMonthlyData() {
        val year = Calendar.getInstance().get(Calendar.YEAR).toString()
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        
        lifecycleScope.launch {
            val allYearEntries = expenseDao.getExpensesForYear(year)
            val monthlyTotals = mutableMapOf<Int, Double>()
            
            allYearEntries.forEach {
                try {
                    val cal = Calendar.getInstance()
                    cal.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.date)!!
                    val month = cal.get(Calendar.MONTH)
                    monthlyTotals[month] = (monthlyTotals[month] ?: 0.0) + it.amount
                } catch (e: Exception) {}
            }
            
            val barEntries = ArrayList<BarEntry>()
            for (i in 0..11) {
                val total = monthlyTotals[i] ?: 0.0
                barEntries.add(BarEntry(i.toFloat(), total.toFloat()))
            }
            
            val dataSet = BarDataSet(barEntries, "").apply {
                color = Color.parseColor("#FF0000")
                valueTextColor = Color.WHITE
            }
            binding.monthlySplitChart.xAxis.valueFormatter = IndexAxisValueFormatter(months)
            binding.monthlySplitChart.xAxis.labelCount = 12
            binding.monthlySplitChart.data = BarData(dataSet)
            
            // Show only 6 bars to force horizontal scrolling
            binding.monthlySplitChart.setVisibleXRangeMaximum(6f)
            binding.monthlySplitChart.moveViewToX(0f)
            binding.monthlySplitChart.invalidate()
        }
    }

    private fun loadYearlyData() {
        lifecycleScope.launch {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val yearsRange = (currentYear..currentYear + 4).toList() // Show 2025-2029 if current is 2025
            val yearlyTotals = mutableMapOf<Int, Double>()
            
            val allEntries = expenseDao.getAllExpenses()
            allEntries.forEach {
                try {
                    val year = it.date.substring(0, 4).toInt()
                    if (year in yearsRange) {
                        yearlyTotals[year] = (yearlyTotals[year] ?: 0.0) + it.amount
                    }
                } catch (e: Exception) {}
            }
            
            val entries = yearsRange.mapIndexed { index, year ->
                Entry(index.toFloat(), yearlyTotals[year]?.toFloat() ?: 0f)
            }
            
            val dataSet = LineDataSet(entries, "5-Year trend").apply {
                color = Color.parseColor("#FF5722")
                setCircleColor(Color.WHITE)
                valueTextColor = Color.WHITE
                lineWidth = 2f
            }
            
            binding.yearlyLineChart.xAxis.valueFormatter = IndexAxisValueFormatter(yearsRange.map { it.toString() }.toTypedArray())
            binding.yearlyLineChart.xAxis.labelCount = 5
            binding.yearlyLineChart.xAxis.granularity = 1f
            binding.yearlyLineChart.data = LineData(dataSet)
            binding.yearlyLineChart.invalidate()
        }
    }

    data class DailyTotal(val date: String, val total: Double)
}