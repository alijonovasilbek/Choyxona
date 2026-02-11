from datetime import datetime, timedelta
from sqlalchemy import select, and_
from database import database
from models.main_models import bookings, users, rooms, BookingStatus
from typing import List
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class LocalNotificationService:
    """Service for managing notification data without external services."""

    async def get_upcoming_bookings(self, hours_before: int = 2) -> List[dict]:
        """
        Get upcoming bookings that need notifications.

        Args:
            hours_before: How many hours before the booking

        Returns:
            List of booking dictionaries
        """
        now = datetime.now()
        target_time = now + timedelta(hours=hours_before)

        # Find bookings that are happening in 'hours_before' hours
        bookings_query = select(
            bookings,
            rooms.c.name.label('room_name')
        ).select_from(
            bookings.join(rooms, bookings.c.room_id == rooms.c.id)
        ).where(
            and_(
                bookings.c.booking_date == target_time.date(),
                bookings.c.booking_time >= target_time.time(),
                bookings.c.booking_time < (target_time + timedelta(hours=1)).time(),
                bookings.c.status.in_([
                    BookingStatus.KUTILMOQDA,
                    BookingStatus.MUVAFFAQIYATLI
                ])
            )
        )

        upcoming_bookings = await database.fetch_all(bookings_query)

        logger.info(f"🔍 Found {len(upcoming_bookings)} upcoming bookings")

        result = []
        for booking in upcoming_bookings:
            result.append({
                "id": booking['id'],
                "room_id": booking['room_id'],
                "room_name": booking['room_name'],
                "booking_date": str(booking['booking_date']),
                "booking_time": str(booking['booking_time']),
                "customer_name": booking['customer_name'],
                "customer_phone": booking['customer_phone'],
                "guest_count": booking['guest_count'],
                "food_description": booking['food_description'],
                "status": booking['status']
            })

        return result

    async def get_all_active_users(self) -> List[dict]:
        """
        Get all active users.

        Returns:
            List of user dictionaries
        """
        users_query = select(
            users.c.id,
            users.c.full_name,
            users.c.username
        ).where(users.c.is_active == True)

        active_users = await database.fetch_all(users_query)

        return [
            {
                "id": user['id'],
                "full_name": user['full_name'],
                "username": user['username']
            }
            for user in active_users
        ]


# Global instance
notification_service = LocalNotificationService()


async def get_notifications_for_users():
    """
    Get upcoming bookings that users should be notified about.
    This can be polled by Android app.
    """
    bookings = await notification_service.get_upcoming_bookings(hours_before=2)
    users = await notification_service.get_all_active_users()

    return {
        "bookings": bookings,
        "users": users,
        "timestamp": datetime.now().isoformat()
    }