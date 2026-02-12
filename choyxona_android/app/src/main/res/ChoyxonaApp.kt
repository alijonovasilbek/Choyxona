class ChoyxonaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "booking_notifications",
                "Booking Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Upcoming booking reminders"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
