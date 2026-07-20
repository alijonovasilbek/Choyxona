package uz.choyxona.app.data.api

import org.json.JSONObject
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Turns raw API/network failures into human-readable Uzbek messages.
 * Known backend `detail` strings are translated; anything unknown falls
 * back to a friendly message by HTTP status code.
 */
object ApiErrorMapper {

    private val KNOWN = listOf(
        "Incorrect username or password" to "Login yoki parol noto'g'ri",
        "Invalid username or password" to "Login yoki parol noto'g'ri",
        "Username already registered" to "Bu login allaqachon band",
        "Phone number already registered" to "Bu telefon raqam ro'yxatdan o'tgan",
        "Registration is closed" to "Ro'yxatdan o'tish yopilgan",
        "Room with this name already exists" to "Bu nomdagi xona allaqachon mavjud",
        "Room is already booked for this date" to "Bu xona tanlangan sanada band",
        "Cannot create booking for past dates" to "O'tgan sana uchun bron yaratib bo'lmaydi",
        "Room is not active" to "Bu xona faol emas",
        "Room does not belong to your active filial" to "Bu xona sizning filialingizga tegishli emas",
        "Booking does not belong to your active filial" to "Bu bron sizning filialingizga tegishli emas",
        "Room not found" to "Xona topilmadi",
        "Booking not found" to "Bron topilmadi",
        "User not found" to "Foydalanuvchi topilmadi",
        "Filial not found" to "Filial topilmadi",
        "Filial is not active" to "Bu filial faol emas",
        "Filial ID is required" to "Filial tanlanishi shart",
        "No active filial selected" to "Filial tanlanmagan. Qaytadan kiring",
        "Oshpaz must be assigned to a filial" to "Oshpazga filial biriktirilmagan",
        "Only Admin and Superadmin can switch filials" to "Filial almashtirishga ruxsat yo'q",
        "Could not validate credentials" to "Sessiya muddati tugagan. Qaytadan kiring",
        "Inactive user" to "Hisobingiz faol emas",
        "Not authenticated" to "Sessiya muddati tugagan. Qaytadan kiring",
        "permission" to "Sizda bu amal uchun ruxsat yo'q"
    )

    fun fromResponse(response: Response<*>): String {
        val detail = try {
            val body = response.errorBody()?.string()
            if (body.isNullOrBlank()) null
            else JSONObject(body).optString("detail").ifBlank { null }
        } catch (e: Exception) {
            null
        }

        detail?.let { d ->
            KNOWN.forEach { (key, uz) ->
                if (d.contains(key, ignoreCase = true)) return uz
            }
        }

        return when (response.code()) {
            400 -> "Ma'lumotlar noto'g'ri kiritilgan. Tekshirib qayta urinib ko'ring"
            401 -> "Sessiya muddati tugagan. Qaytadan kiring"
            403 -> "Sizda bu amal uchun ruxsat yo'q"
            404 -> "Ma'lumot topilmadi"
            409 -> "Bu ma'lumot allaqachon mavjud"
            422 -> "Ma'lumotlar to'liq emas. Barcha maydonlarni to'ldiring"
            in 500..599 -> "Serverda xatolik. Birozdan so'ng qayta urinib ko'ring"
            else -> "Xatolik yuz berdi. Qayta urinib ko'ring"
        }
    }

    fun fromThrowable(t: Throwable): String = when (t) {
        is SocketTimeoutException -> "Server javob bermayapti. Internetni tekshiring"
        is IOException -> "Internet aloqasi yo'q. Ulanishni tekshiring"
        else -> t.message ?: "Xatolik yuz berdi. Qayta urinib ko'ring"
    }
}
