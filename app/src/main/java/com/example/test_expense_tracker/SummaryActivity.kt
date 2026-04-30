package com.example.test_expense_tracker

import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.res.ResourcesCompat
import com.example.test_expense_tracker.data.ExpenseDao
import com.example.test_expense_tracker.data.ExpenseDatabase
import com.example.test_expense_tracker.data.ExpenseEntry
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
    }

    private fun setupCharts() {
        var typeface: android.graphics.Typeface? = null
        try {
            typeface = ResourcesCompat.getFont(this, R.font.normal_font)
        } catch (e: Exception) {
            // Font not ready or missing
        }
        
        val charts = listOf(binding.weeklyBarChart, binding.monthlySplitChart, binding.yearlyLineChart)
        charts.forEach { chart ->
            chart.description.isEnabled = false
            chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
            chart.xAxis.textColor = Color.WHITE
            chart.xAxis.setDrawGridLines(false)
            if (typeface != null) chart.xAxis.typeface = typeface
            chart.axisLeft.textColor = Color.WHITE
            if (typeface != null) chart.axisLeft.typeface = typeface
            chart.axisRight.isEnabled = false
            chart.legend.isEnabled = false
            chart.setNoDataTextColor(Color.GRAY)
        }
        
        // Enable touch and scrolling for monthly chart
        binding.monthlySplitChart.setTouchEnabled(true)
        binding.monthlySplitChart.isDragEnabled = true
        binding.monthlySplitChart.setScaleEnabled(false)
        binding.monthlySplitChart.setPinchZoom(false)
    }

    private fun loadAllData() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        binding.tvMonthlyTitle.text = "MONTHLY FLOW ($currentYear)"
        
        loadWeeklyData()
        loadMonthlyData()
        loadYearlyData()
        loadDailyBreakdown()
    }

    private fun loadWeeklyData() {
        val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(weeklyCalendar.time).uppercase()
        binding.tvWeeklyTitle.text = "WEEKLY OVERVIEW ($monthName)"

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displaySdfDay = SimpleDateFormat("EEE", Locale.getDefault())
        val displaySdfDate = SimpleDateFormat("dd", Locale.getDefault())
        val cal = weeklyCalendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        
        val weekEntries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        
        lifecycleScope.launch {
            for (i in 0..6) {
                val dateStr = sdf.format(cal.time)
                labels.add("${displaySdfDay.format(cal.time)}\n${displaySdfDate.format(cal.time)}")
                val entries = expenseDao.getExpensesByDate(dateStr)
                val total = entries.sumOf { it.amount }
                weekEntries.add(BarEntry(i.toFloat(), total.toFloat()))
                cal.add(Calendar.DATE, 1)
            }
            
            val dataSet = BarDataSet(weekEntries, "").apply {
                color = Color.parseColor("#FF0000") // Red
                valueTextColor = Color.WHITE
            }
            binding.weeklyBarChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            binding.weeklyBarChart.xAxis.labelCount = 7
            binding.weeklyBarChart.xAxis.setLabelRotationAngle(0f) // Keep horizontal
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
                val cal = Calendar.getInstance()
                cal.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.date)!!
                val month = cal.get(Calendar.MONTH)
                monthlyTotals[month] = (monthlyTotals[month] ?: 0.0) + it.amount
            }
            
            val barEntries = ArrayList<BarEntry>()
            for (i in 0..11) {
                val total = monthlyTotals[i] ?: 0.0
                barEntries.add(BarEntry(i.toFloat(), total.toFloat()))
            }
            
            val dataSet = BarDataSet(barEntries, "Monthly Expenses").apply {
                color = Color.parseColor("#FF0000")
                valueTextColor = Color.WHITE
            }
            binding.monthlySplitChart.xAxis.valueFormatter = IndexAxisValueFormatter(months)
            binding.monthlySplitChart.xAxis.labelCount = 12
            binding.monthlySplitChart.data = BarData(dataSet)
            
            // Show 6 months at a time, scroll for the rest
            binding.monthlySplitChart.setVisibleXRangeMaximum(6f)
            binding.monthlySplitChart.moveViewToX(0f)
            
            binding.monthlySplitChart.invalidate()
        }
    }

    private fun loadYearlyData() {
        lifecycleScope.launch {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val yearsRange = (currentYear - 4..currentYear).toList()
            val yearlyTotals = mutableMapOf<Int, Double>()
            
            val allEntries = expenseDao.getAllExpenses()
            allEntries.forEach {
                val year = it.date.substring(0, 4).toInt()
                if (year in yearsRange) {
                    yearlyTotals[year] = (yearlyTotals[year] ?: 0.0) + it.amount
                }
            }
            
            val entries = yearsRange.mapIndexed { index, year ->
                Entry(index.toFloat(), yearlyTotals[year]?.toFloat() ?: 0f)
            }
            
            val dataSet = LineDataSet(entries, "5-Year Trend").apply {
                color = Color.parseColor("#FF5722") // Orange
                setCircleColor(Color.WHITE)
                valueTextColor = Color.WHITE
                lineWidth = 2f
            }
            
            binding.yearlyLineChart.xAxis.valueFormatter = IndexAxisValueFormatter(yearsRange.map { it.toString() }.toTypedArray())
            binding.yearlyLineChart.xAxis.labelCount = yearsRange.size
            binding.yearlyLineChart.xAxis.granularity = 1f
            binding.yearlyLineChart.xAxis.isGranularityEnabled = true
            binding.yearlyLineChart.data = LineData(dataSet)
            binding.yearlyLineChart.invalidate()
        }
    }

    private fun loadDailyBreakdown() {
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()) + "%"
        lifecycleScope.launch {
            val monthlyEntries = expenseDao.getExpensesForMonth(monthKey)
            val dailyTotals = monthlyEntries.groupBy { it.date }
                .map { (date, entries) -> DailyTotal(date, entries.sumOf { it.amount }) }
                .sortedByDescending { it.date }
            
            binding.rvDailyExpenses.layoutManager = LinearLayoutManager(this@SummaryActivity)
            binding.rvDailyExpenses.adapter = DailyTotalAdapter(dailyTotals)
        }
    }

    data class DailyTotal(val date: String, val total: Double)
}