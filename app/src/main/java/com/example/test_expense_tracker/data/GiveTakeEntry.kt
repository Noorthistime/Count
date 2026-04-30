package com.example.test_expense_tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "give_take_entries")
data class GiveTakeEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "GIVE" or "TAKE"
    val amount: Double,
    val personName: String,
    val reason: String,
    val date: String // Format: yyyy-MM-dd
)