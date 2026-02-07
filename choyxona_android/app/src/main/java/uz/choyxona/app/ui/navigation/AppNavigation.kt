package uz.choyxona.app.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object Bookings : Screen("bookings")
    object Rooms : Screen("rooms")
    object Reports : Screen("reports")
    object BookingDetail : Screen("booking_detail/{bookingId}") {
        fun createRoute(bookingId: Int) = "booking_detail/$bookingId"
    }
    object RoomDetail : Screen("room_detail/{roomId}") {
        fun createRoute(roomId: Int) = "room_detail/$roomId"
    }
    object CreateBooking : Screen("create_booking")
    object CreateRoom : Screen("create_room")
}