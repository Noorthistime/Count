package com.example.test_expense_tracker

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.test_expense_tracker.data.ExpenseDao
import com.example.test_expense_tracker.data.ExpenseDatabase
import com.example.test_expense_tracker.data.GiveTakeEntry
import com.example.test_expense_tracker.databinding.ActivityGiveTakeBinding
import com.example.test_expense_tracker.databinding.DialogAddGiveTakeBinding
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class GiveTakeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGiveTakeBinding
    private lateinit var expenseDao: ExpenseDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGiveTakeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        expenseDao = ExpenseDatabase.getDatabase(this).expenseDao()

        setupViewPager()
        setupUI()
    }

    private fun setupViewPager() {
        val adapter = GiveTakePagerAdapter(this)
        binding.viewPager.adapter = adapter
        
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = if (position == 0) "GIVE" else "TAKE"
        }.attach()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }
        
        binding.fabAdd.setOnClickListener {
            val currentType = if (binding.viewPager.currentItem == 0) "GIVE" else "TAKE"
            showAddDialog(currentType)
        }
    }

    private fun showAddDialog(type: String) {
        val dialogBinding = DialogAddGiveTakeBinding.inflate(LayoutInflater.from(this))

        AlertDialog.Builder(this)
            .setTitle("Add $type Entry")
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { _, _ ->
                val name = dialogBinding.etPersonName.text.toString()
                val amount = dialogBinding.etGtAmount.text.toString().toDoubleOrNull() ?: 0.0
                val reason = dialogBinding.etGtReason.text.toString()
                
                if (name.isNotBlank() && amount > 0) {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val newEntry = GiveTakeEntry(type = type, amount = amount, personName = name, reason = reason, date = date)
                    
                    lifecycleScope.launch {
                        expenseDao.insertGiveTake(newEntry)
                        refreshVisibleFragment()
                    }
                } else {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refreshVisibleFragment() {
        val fragment = supportFragmentManager.findFragmentByTag("f" + binding.viewPager.currentItem)
        if (fragment is GiveTakeFragment) {
            fragment.loadData()
        }
    }
}