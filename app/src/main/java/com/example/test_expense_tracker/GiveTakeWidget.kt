package com.example.test_expense_tracker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class GiveTakeWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.give_take_widget)

        val theme = ThemeStorage.getTheme(context)
        val colorRes = ThemeStorage.getThemeColorRes(theme)
        val color = context.getColor(colorRes)
        views.setInt(R.id.btn_widget_give, "setBackgroundColor", color)
        views.setInt(R.id.btn_widget_take, "setBackgroundColor", color)

        // GIVE button intent
        val giveIntent = Intent(context, GiveTakeActivity::class.java).apply {
            putExtra("EXTRA_TYPE", "GIVE")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val givePendingIntent = PendingIntent.getActivity(
            context, 0, giveIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_widget_give, givePendingIntent)

        // TAKE button intent
        val takeIntent = Intent(context, GiveTakeActivity::class.java).apply {
            putExtra("EXTRA_TYPE", "TAKE")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val takePendingIntent = PendingIntent.getActivity(
            context, 1, takeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_widget_take, takePendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}