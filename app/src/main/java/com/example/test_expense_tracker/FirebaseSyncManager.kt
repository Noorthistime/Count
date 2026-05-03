package com.example.test_expense_tracker

import com.example.test_expense_tracker.data.ExpenseDao
import com.example.test_expense_tracker.data.ExpenseEntry
import com.example.test_expense_tracker.data.GiveTakeEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseSyncManager(private val expenseDao: ExpenseDao) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    suspend fun syncData() {
        val user = auth.currentUser ?: return
        val userId = user.uid

        // 1. Sync Expense Entries
        val localExpenses = expenseDao.getAllExpenses()
        val expenseRef = db.collection("users").document(userId).collection("expenses")
        
        // Upload local to cloud (Simple overwrite for now to keep it minimal)
        for (expense in localExpenses) {
            expenseRef.document(expense.id.toString()).set(expense).await()
        }
        
        // Download cloud to local
        val cloudExpenses = expenseRef.get().await()
        for (doc in cloudExpenses.documents) {
            val expense = doc.toObject(ExpenseEntry::class.java)
            if (expense != null) {
                expenseDao.insertExpense(expense)
            }
        }

        // 2. Sync Give/Take Entries
        val giveEntries = expenseDao.getGiveTakeByType("GIVE")
        val takeEntries = expenseDao.getGiveTakeByType("TAKE")
        val gtRef = db.collection("users").document(userId).collection("give_take")

        for (entry in giveEntries + takeEntries) {
            gtRef.document(entry.id.toString()).set(entry).await()
        }

        val cloudGT = gtRef.get().await()
        for (doc in cloudGT.documents) {
            val entry = doc.toObject(GiveTakeEntry::class.java)
            if (entry != null) {
                expenseDao.insertGiveTake(entry)
            }
        }
    }
}