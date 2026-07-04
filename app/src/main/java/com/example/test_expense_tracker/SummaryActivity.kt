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
        val theme = ThemeStorage.getTheme(this)
        setTheme(ThemeStorage.getThemeResource(theme))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                kotlin.math.max(systemBars.bottom, ime.bottom)
            )
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
        val startDate = sdf.format(cal.time)
        
        val calEnd = cal.clone() as Calendar
        calEnd.add(Calendar.DATE, 6)
        val endDate = sdf.format(calEnd.time)
        
        val weekEntries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        
        lifecycleScope.launch {
            val allEntries = expenseDao.getExpensesInRange(startDate, endDate)
            val dateTotals = allEntries.groupBy { it.date }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            val tempCal = weeklyCalendar.clone() as Calendar
            tempCal.set(Calendar.DAY_OF_WEEK, tempCal.firstDayOfWeek)
            
            for (i in 0..6) {
                val dateStr = sdf.format(tempCal.time)
                val total = dateTotals[dateStr] ?: 0.0
                weekEntries.add(BarEntry(i.toFloat(), total.toFloat()))
                labels.add("${dayFormat.format(tempCal.time)}-${dateFormat.format(tempCal.time)}")
                tempCal.add(Calendar.DATE, 1)
            }
            
            val dataSet = BarDataSet(weekEntries, "").apply {
                color = ThemeStorage.getColorPrimary(this@SummaryActivity)
                valueTextColor = Color.WHITE
                valueTextSize = 10f
            }
            binding.weeklyBarChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            binding.weeklyBarChart.xAxis.granularity = 1f
            binding.weeklyBarChart.xAxis.labelCount = 7
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
                    // Optimized: Use substring instead of parsing the whole date
                    val month = it.date.substring(5, 7).toInt() - 1
                    monthlyTotals[month] = (monthlyTotals[month] ?: 0.0) + it.amount
                } catch (e: Exception) {}
            }
            
            val barEntries = ArrayList<BarEntry>()
            for (i in 0..11) {
                val total = monthlyTotals[i] ?: 0.0
                barEntries.add(BarEntry(i.toFloat(), total.toFloat()))
            }
            
            val dataSet = BarDataSet(barEntries, "").apply {
                color = ThemeStorage.getColorPrimary(this@SummaryActivity)
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
                color = ThemeStorage.getColorPrimary(this@SummaryActivity)
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