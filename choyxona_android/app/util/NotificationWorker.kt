package uz.choyxona.app.util



val response = ApiClient.notifications.getUpcomingNotifications(
    token = "Bearer $accessToken",
    hoursBefore = 2
)

val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
    15, TimeUnit.HOURS,
    5, TimeUnit.MINUTES
)

class NotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("NotificationWorker", "🔔 Checking notifications")

        try {
            // 1. /notifications/check
            val hasNotifications = ApiClient.api.checkNotifications()

            if (hasNotifications.has_notifications) {
                // 2. /notifications/upcoming
                val response = ApiClient.api.getUpcomingNotifications()

                showNotification(response.count)
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e("NotificationWorker", "❌ Error", e)
            return Result.retry()
        }
    }

    private fun showNotification(count: Int) {
        val notification = NotificationCompat.Builder(
            applicationContext,
            "booking_notifications"
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Upcoming bookings")
            .setContentText("You have $count upcoming bookings")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat
            .from(applicationContext)
            .notify(1001, notification)
    }
}
