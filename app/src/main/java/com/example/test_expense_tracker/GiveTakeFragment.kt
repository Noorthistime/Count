package com.example.test_expense_tracker

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.test_expense_tracker.data.ExpenseDao
import com.example.test_expense_tracker.data.ExpenseDatabase
import com.example.test_expense_tracker.data.GiveTakeEntry
import com.example.test_expense_tracker.databinding.DialogAddGiveTakeBinding
import com.example.test_expense_tracker.databinding.FragmentGiveTakeBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class GiveTakeFragment : Fragment() {

    private var _binding: FragmentGiveTakeBinding? = null
    private val binding get() = _binding!!
    private lateinit var expenseDao: ExpenseDao
    private lateinit var adapter: GiveTakeAdapter
    private var type: String = "GIVE"

    companion object {
        private const val ARG_TYPE = "type"
        fun newInstance(type: String) = GiveTakeFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TYPE, type)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = arguments?.getString(ARG_TYPE) ?: "GIVE"
        expenseDao = ExpenseDatabase.getDatabase(requireContext()).expenseDao()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGiveTakeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadData()
    }

    private fun setupRecyclerView() {
        adapter = GiveTakeAdapter(emptyList(),
            onEdit = { entry -> showAddEditDialog(entry) },
            onDelete = { entry -> deleteEntry(entry) }
        )
        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = adapter
    }

    fun loadData() {
        lifecycleScope.launch {
            val entries = expenseDao.getGiveTakeByType(type)
            adapter.updateData(entries)
        }
    }

    private fun showAddEditDialog(entry: GiveTakeEntry?) {
        val dialogBinding = DialogAddGiveTakeBinding.inflate(LayoutInflater.from(requireContext()))
        val isEdit = entry != null
        
        if (isEdit) {
            dialogBinding.etPersonName.setText(entry?.personName)
            dialogBinding.etGtAmount.setText(entry?.amount.toString())
            dialogBinding.etGtReason.setText(entry?.reason)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (isEdit) "Edit Entry" else "Add $type Entry")
            .setView(dialogBinding.root)
            .setPositiveButton(if (isEdit) "Update" else "Add") { _, _ ->
                val name = dialogBinding.etPersonName.text.toString()
                val amount = dialogBinding.etGtAmount.text.toString().toDoubleOrNull() ?: 0.0
                val reason = dialogBinding.etGtReason.text.toString()
                
                if (name.isNotBlank() && amount > 0) {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val newEntry = if (isEdit) {
                        entry!!.copy(personName = name, amount = amount, reason = reason)
                    } else {
                        GiveTakeEntry(type = type, amount = amount, personName = name, reason = reason, date = date)
                    }
                    
                    lifecycleScope.launch {
                        if (isEdit) expenseDao.updateGiveTake(newEntry)
                        else expenseDao.insertGiveTake(newEntry)
                        loadData()
                    }
                } else {
                    Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteEntry(entry: GiveTakeEntry) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Entry")
            .setMessage("Are you sure?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    expenseDao.deleteGiveTake(entry)
                    loadData()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}