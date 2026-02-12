package uz.choyxona.app.data.model

data class CheckResponse(
    val has_notifications: Boolean,
    val count: Int,
    val timestamp: String
)
