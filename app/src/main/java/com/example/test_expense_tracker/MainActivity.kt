package com.example.test_expense_tracker

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.test_expense_tracker.data.ExpenseDao
import com.example.test_expense_tracker.data.ExpenseDatabase
import com.example.test_expense_tracker.data.ExpenseEntry
import com.example.test_expense_tracker.databinding.ActivityMainBinding
import com.example.test_expense_tracker.databinding.DialogEditExpenseBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var expenseDao: ExpenseDao
    private lateinit var adapter: ExpenseEntryAdapter
    private var selectedDate = Calendar.getInstance()
    private var highlightedDates = setOf<String>()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        val theme = ThemeStorage.getTheme(this)
        setTheme(ThemeStorage.getThemeResource(theme))
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
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

        setupBackNavigation()
        expenseDao = ExpenseDatabase.getDatabase(this).expenseDao()

        setupRecyclerView()
        setupUI()
        loadHighlightedDates()
        loadDataForSelectedDate()
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.etAmount.hasFocus() || binding.etDescription.hasFocus()) {
                    binding.etAmount.clearFocus()
                    binding.etDescription.clearFocus()
                } else {
                    val today = Calendar.getInstance()
                    if (dateFormatter.format(selectedDate.time) != dateFormatter.format(today.time)) {
                        selectedDate = today
                        loadDataForSelectedDate()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }

    private fun loadHighlightedDates() {
        lifecycleScope.launch {
            highlightedDates = expenseDao.getDatesWithExpenses().toSet()
        }
    }

    private fun setupRecyclerView() {
        adapter = ExpenseEntryAdapter(emptyList(), 
            onEdit = { entry -> showEditDialog(entry) },
            onDelete = { entry -> deleteEntry(entry) }
        )
        binding.rvEntries.layoutManager = LinearLayoutManager(this)
        binding.rvEntries.adapter = adapter
        binding.rvEntries.setHasFixedSize(true)
    }

    private fun setupUI() {
        binding.btnCalendar.setOnClickListener { showDatePicker() }
        binding.btnSummary.setOnClickListener {
            val intent = android.content.Intent(this, SummaryActivity::class.java)
            startActivity(intent)
        }
        binding.btnGiveTake.setOnClickListener {
            val intent = android.content.Intent(this, GiveTakeActivity::class.java)
            startActivity(intent)
        }
        binding.btnSync.setOnClickListener {
            val intent = android.content.Intent(this, SyncActivity::class.java)
            startActivity(intent)
        }
        binding.btnPalette.setOnClickListener { showThemeSelectionDialog() }
        binding.btnAdd.setOnClickListener { addExpense() }
    }

    @androidx.annotation.OptIn(com.google.android.material.badge.ExperimentalBadgeUtils::class)
    private fun showDatePicker() {
        val constraintsBuilder = CalendarConstraints.Builder()
            .setEnd(System.currentTimeMillis())
        
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("SELECT DATE")
            .setSelection(selectedDate.timeInMillis)
            .setCalendarConstraints(constraintsBuilder.build())
            .setDayViewDecorator(ExpenseDayDecorator(highlightedDates))
            .setTheme(com.google.android.material.R.style.ThemeOverlay_Material3_MaterialCalendar)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            handleDateSelection(selection)
        }
        
        datePicker.show(supportFragmentManager, "DATE_PICKER")
    }

    private fun handleDateSelection(selection: Long?) {
        selection?.let {
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = it
            
            val localCalendar = Calendar.getInstance()
            localCalendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            
            selectedDate = localCalendar
            loadDataForSelectedDate()
        }
    }

    private fun loadDataForSelectedDate() {
        val dateString = dateFormatter.format(selectedDate.time)
        binding.tvDate.text = displayFormatter.format(selectedDate.time)

        lifecycleScope.launch {
            val entries = expenseDao.getExpensesByDate(dateString)
            adapter.updateData(entries)
            
            val total = entries.sumOf { it.amount }
            binding.tvTotalAmount.text = String.format(Locale.getDefault(), "₹%.2f", total)
        }
    }

    private fun addExpense() {
        val amountStr = binding.etAmount.text.toString()
        val note = binding.etDescription.text.toString()

        if (amountStr.isBlank() || note.isBlank()) {
            Toast.makeText(this, "Please enter both amount and description", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val dateString = dateFormatter.format(selectedDate.time)
        val entry = ExpenseEntry(date = dateString, amount = amount, note = note)

        lifecycleScope.launch {
            expenseDao.insertExpense(entry)
            binding.etAmount.text?.clear()
            binding.etDescription.text?.clear()
            loadHighlightedDates()
            loadDataForSelectedDate()
            Toast.makeText(this@MainActivity, "Expense added", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditDialog(entry: ExpenseEntry) {
        val dialogBinding = DialogEditExpenseBinding.inflate(LayoutInflater.from(this))
        dialogBinding.etEditAmount.setText(entry.amount.toString())
        dialogBinding.etEditNote.setText(entry.note)

        AlertDialog.Builder(this)
            .setTitle("Edit Expense")
            .setView(dialogBinding.root)
            .setPositiveButton("Update") { _, _ ->
                val amount = dialogBinding.etEditAmount.text.toString().toDoubleOrNull() ?: entry.amount
                val note = dialogBinding.etEditNote.text.toString()
                
                if (note.isNotBlank() && amount > 0) {
                    lifecycleScope.launch {
                        expenseDao.updateExpense(entry.copy(amount = amount, note = note))
                        loadHighlightedDates()
                        loadDataForSelectedDate()
                        Toast.makeText(this@MainActivity, "Entry updated", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteEntry(entry: ExpenseEntry) {
        AlertDialog.Builder(this)
            .setTitle("Delete Entry")
            .setMessage("Are you sure you want to delete this expense?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    expenseDao.deleteExpense(entry)
                    loadHighlightedDates()
                    loadDataForSelectedDate()
                    Toast.makeText(this@MainActivity, "Entry deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showThemeSelectionDialog() {
        val themes = arrayOf("Default Orange", "Nothing Red", "Ethereal Blue", "Contrast Green", "Balanced Grey")
        val themeKeys = arrayOf(ThemeStorage.THEME_ORANGE, ThemeStorage.THEME_RED, ThemeStorage.THEME_BLUE, ThemeStorage.THEME_GREEN, ThemeStorage.THEME_GREY)
        
        val currentTheme = ThemeStorage.getTheme(this)
        val checkedItem = themeKeys.indexOf(currentTheme)

        AlertDialog.Builder(this)
            .setTitle("Select App Color")
            .setSingleChoiceItems(themes, checkedItem) { dialog, which ->
                val selectedTheme = themeKeys[which]
                if (selectedTheme != currentTheme) {
                    ThemeStorage.saveTheme(this, selectedTheme)
                    recreate()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}


@com.google.android.material.badge.ExperimentalBadgeUtils
class ExpenseDayDecorator(private val highlightedDates: Set<String>) : com.google.android.material.datepicker.DayViewDecorator() {
    
    constructor(parcel: android.os.Parcel) : this(
        mutableListOf<String>().run {
            parcel.readStringList(this)
            this.toSet()
        }
    )

    override fun getBackgroundColor(
        context: android.content.Context,
        year: Int,
        month: Int,
        day: Int,
        valid: Boolean,
        selected: Boolean
    ): android.content.res.ColorStateList? {
        val today = Calendar.getInstance()
        val isToday = today.get(Calendar.YEAR) == year &&
                today.get(Calendar.MONTH) == month &&
                today.get(Calendar.DAY_OF_MONTH) == day

        val dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day)

        return when {
            isToday -> android.content.res.ColorStateList.valueOf(context.getColor(R.color.nothing_grey_medium))
            highlightedDates.contains(dateStr) -> {
                val color = ThemeStorage.getColorPrimary(context)
                // Apply 30% alpha (0x4D)
                val alphaColor = (color and 0x00FFFFFF) or 0x4D000000
                android.content.res.ColorStateList.valueOf(alphaColor)
            }
            else -> null
        }
    }

    override fun writeToParcel(parcel: android.os.Parcel, flags: Int) {
        parcel.writeStringList(highlightedDates.toList())
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : android.os.Parcelable.Creator<ExpenseDayDecorator> {
        override fun createFromParcel(parcel: android.os.Parcel): ExpenseDayDecorator = ExpenseDayDecorator(parcel)

        override fun newArray(size: Int): Array<ExpenseDayDecorator?> = arrayOfNulls(size)
    }
}