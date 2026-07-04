package com.example.test_expense_tracker

import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.test_expense_tracker.data.ExpenseDao
import com.example.test_expense_tracker.data.ExpenseDatabase
import com.example.test_expense_tracker.data.ExpenseEntry
import com.example.test_expense_tracker.databinding.ActivityQuickAddBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class QuickAddActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuickAddBinding
    private lateinit var expenseDao: ExpenseDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuickAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle back button to go to MainActivity instead of exiting
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateToMain()
            }
        })

        // Make it look like a dialog
        window.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        expenseDao = ExpenseDatabase.getDatabase(this).expenseDao()

        binding.btnQuickSave.setOnClickListener {
            saveExpense()
        }
    }

    private fun navigateToMain() {
        val intent = android.content.Intent(this, MainActivity::class.java)
        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    private fun saveExpense() {
        val amountStr = binding.etQuickAmount.text.toString()
        val note = binding.etQuickDescription.text.toString()

        if (amountStr.isBlank() || note.isBlank()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Valid amount required", Toast.LENGTH_SHORT).show()
            return
        }

        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val entry = ExpenseEntry(date = date, amount = amount, note = note)

        lifecycleScope.launch {
            expenseDao.insertExpense(entry)
            Toast.makeText(this@QuickAddActivity, "Expense Saved", Toast.LENGTH_SHORT).show()
            navigateToMain()
        }
    }
}