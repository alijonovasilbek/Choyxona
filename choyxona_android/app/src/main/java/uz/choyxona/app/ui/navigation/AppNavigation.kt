package uz.choyxona.app.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object Bookings : Screen("bookings")
    object WeeklyBookings : Screen("weekly_bookings")
    object Rooms : Screen("rooms")
    object Reports : Screen("reports")
    object Users : Screen("users")

    object BookingDetail : Screen("booking_detail/{bookingId}") {
        fun createRoute(bookingId: Int) = "booking_detail/$bookingId"
    }
    object RoomDetail : Screen("room_detail/{roomId}") {
        fun createRoute(roomId: Int) = "room_detail/$roomId"
    }

    object CreateBooking : Screen("create_booking")
    object EditBooking : Screen("edit_booking/{bookingId}") {
        fun createRoute(bookingId: Int) = "edit_booking/$bookingId"
    }

    object CreateRoom : Screen("create_room")
    object EditRoom : Screen("edit_room/{roomId}") {
        fun createRoute(roomId: Int) = "edit_room/$roomId"
    }

    object CreateUser : Screen("create_user")
    object EditUser : Screen("edit_user/{userId}") {
        fun createRoute(userId: Int) = "edit_user/$userId"
    }
}