package ru.akarakuts.tictactoe.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.akarakuts.tictactoe.MainActivity
import ru.akarakuts.tictactoe.R
import ru.akarakuts.tictactoe.progress.GameProgressStore

/**
 * Home-screen widget: shows the app name, a one-line vs-AI stat, and a "New Game" button that
 * launches [MainActivity]. Stats are read from the persisted progress (DataStore) and refreshed
 * on every update / data broadcast.
 */
class HomeWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id -> renderAsync(context, appWidgetManager, id) }
    }

    /** Re-render a single widget instance after the progress store changes. */
    fun refresh(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(componentName(context))
        ids.forEach { id -> renderAsync(context, manager, id) }
    }

    private fun renderAsync(context: Context, manager: AppWidgetManager, id: Int) {
        scope.launch {
            val progress = GameProgressStore(context).load()
            val views = buildViews(context, wins = progress.winsVsAI, streak = progress.currentWinStreak)
            manager.updateAppWidget(id, views)
        }
    }

    private fun buildViews(context: Context, wins: Int, streak: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_home)
        views.setTextViewText(
            R.id.widget_stat,
            context.getString(R.string.widget_stat_format, wins, streak),
        )
        // The whole widget + the button both open the app; the button is purely a visual affordance.
        val openIntent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        views.setOnClickPendingIntent(R.id.widget_button, pending)
        views.setOnClickPendingIntent(android.R.id.background, pending)
        return views
    }

    private fun componentName(context: Context) =
        android.content.ComponentName(context, HomeWidgetProvider::class.java)

    companion object {
        /** Call after a finished round so the widget reflects fresh stats without waiting for a system update. */
        fun broadcastUpdate(context: Context) {
            val intent = Intent(context, HomeWidgetProvider::class.java)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            context.sendBroadcast(intent)
        }
    }
}
