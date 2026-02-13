package uz.choyxona.app.util

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.choyxona.app.R
import uz.choyxona.app.data.api.ApiClient
import uz.choyxona.app.data.model.CheckResponse
import uz.choyxona.app.data.model.UpcomingResponse
import java.util.concurrent.TimeUnit

class NotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("NotificationWorker", "🔔 Checking notifications")

        return withContext(Dispatchers.IO) {
            try {
                // 1. /notifications/check
                val hasNotifications: CheckResponse = ApiClient.notifications.checkNotifications()

                if (hasNotifications.has_notifications) {
                    Log.d("NotificationWorker", "📬 Found ${hasNotifications.count} notifications")

                    // 2. /notifications/upcoming
                    val response: UpcomingResponse = ApiClient.notifications.getUpcomingNotifications(hoursBefore = 2)

                    showNotification(response.count)
                } else {
                    Log.d("NotificationWorker", "✅ No notifications")
                }

                Result.success()
            } catch (e: Exception) {
                Log.e("NotificationWorker", "❌ Error checking notifications", e)
                Result.retry()
            }
        }
    }

    private fun showNotification(count: Int) {
        try {
            val notification = NotificationCompat.Builder(
                applicationContext,
                "booking_notifications"
            )
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Tez orada keladigan bronlar")
                .setContentText("Sizda $count ta tez orada keladigan bronlar bor")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat
                .from(applicationContext)
                .notify(1001, notification)

            Log.d("NotificationWorker", "📢 Notification shown: $count bookings")
        } catch (e: Exception) {
            Log.e("NotificationWorker", "❌ Error showing notification", e)
        }
    }
}