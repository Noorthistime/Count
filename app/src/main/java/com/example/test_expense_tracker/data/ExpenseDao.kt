package com.example.test_expense_tracker.data

import androidx.room.*

@Dao
interface ExpenseDao {
    // Expense Entries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntry)

    @Update
    suspend fun updateExpense(expense: ExpenseEntry)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntry)

    @Query("SELECT * FROM expense_entries WHERE date = :date ORDER BY id ASC")
    suspend fun getExpensesByDate(date: String): List<ExpenseEntry>

    @Query("SELECT * FROM expense_entries WHERE date LIKE :monthQuery ORDER BY date ASC")
    suspend fun getExpensesForMonth(monthQuery: String): List<ExpenseEntry>

    @Query("SELECT * FROM expense_entries WHERE date LIKE :year || '%' ORDER BY date ASC")
    suspend fun getExpensesForYear(year: String): List<ExpenseEntry>

    @Query("SELECT * FROM expense_entries ORDER BY date ASC")
    suspend fun getAllExpenses(): List<ExpenseEntry>

    @Query("SELECT date FROM expense_entries GROUP BY date HAVING SUM(amount) > 0")
    suspend fun getDatesWithExpenses(): List<String>

    // Give/Take Entries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGiveTake(entry: GiveTakeEntry)

    @Update
    suspend fun updateGiveTake(entry: GiveTakeEntry)

    @Delete
    suspend fun deleteGiveTake(entry: GiveTakeEntry)

    @Query("SELECT * FROM give_take_entries WHERE type = :type ORDER BY date DESC")
    suspend fun getGiveTakeByType(type: String): List<GiveTakeEntry>
}