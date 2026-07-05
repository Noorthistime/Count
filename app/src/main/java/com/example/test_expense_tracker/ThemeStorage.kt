package com.example.test_expense_tracker

import android.content.Context
import android.content.SharedPreferences

object ThemeStorage {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "selected_theme"

    const val THEME_ORANGE = "orange"
    const val THEME_RED = "red"
    const val THEME_BLUE = "blue"
    const val THEME_GREEN = "green"
    const val THEME_GREY = "grey"

    fun saveTheme(context: Context, theme: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, theme).apply()
    }

    fun getTheme(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_THEME, THEME_ORANGE) ?: THEME_ORANGE
    }

    fun getThemeResource(theme: String): Int {
        return when (theme) {
            THEME_RED -> R.style.Theme_Test_Expense_Tracker_Red
            THEME_BLUE -> R.style.Theme_Test_Expense_Tracker_Blue
            THEME_GREEN -> R.style.Theme_Test_Expense_Tracker_Green
            THEME_GREY -> R.style.Theme_Test_Expense_Tracker_Grey
            else -> R.style.Theme_Test_Expense_Tracker // Default Orange
        }
    }

    fun getThemeColorRes(theme: String): Int {
        return when (theme) {
            THEME_RED -> R.color.theme_nothing_red
            THEME_BLUE -> R.color.theme_ethereal_blue
            THEME_GREEN -> R.color.theme_contrast_green
            THEME_GREY -> R.color.theme_balanced_grey
            else -> R.color.nothing_orange
        }
    }

    fun getColorPrimary(context: Context): Int {
        val typedValue = android.util.TypedValue()
        context.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
        return typedValue.data
    }
}