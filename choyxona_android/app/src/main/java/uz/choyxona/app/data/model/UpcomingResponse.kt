package uz.choyxona.app.data.model

data class UpcomingResponse(
    val status: String,
    val count: Int,
    val bookings: List<Any>, // hozircha shunday
    val timestamp: String
)
