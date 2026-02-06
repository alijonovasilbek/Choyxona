from telegram import Bot
from telegram.error import TelegramError
from config import TELEGRAM_BOT_TOKEN
from datetime import datetime, timedelta
from sqlalchemy import select, and_
from database import database
from models.main_models import bookings, users, user_roles, rooms, UserRole, BookingStatus
import asyncio
from typing import List


class TelegramNotificationService:
    """Service for sending Telegram notifications to users."""
    
    def __init__(self, bot_token: str = TELEGRAM_BOT_TOKEN):
        """Initialize Telegram bot."""
        if bot_token:
            self.bot = Bot(token=bot_token)
            self.enabled = True
        else:
            self.bot = None
            self.enabled = False
            print("⚠️  Telegram Bot Token not configured. Notifications disabled.")
    
    async def send_message(self, chat_id: str, message: str) -> bool:
        """
        Send message to a Telegram chat.
        
        Args:
            chat_id: Telegram chat ID
            message: Message text
            
        Returns:
            True if message sent successfully, False otherwise
        """
        if not self.enabled:
            print(f"⚠️  Telegram notifications disabled. Would send to {chat_id}: {message}")
            return False
        
        try:
            await self.bot.send_message(chat_id=chat_id, text=message)
            print(f"✅ Telegram message sent to {chat_id}")
            return True
        except TelegramError as e:
            print(f"❌ Failed to send Telegram message to {chat_id}: {e}")
            return False
    
    async def notify_booking_reminder(self, booking_id: int, hours_before: int = 2):
        """
        Send booking reminder to all admins and cooks.
        
        Args:
            booking_id: ID of the booking
            hours_before: How many hours before the booking to send reminder
        """
        # Fetch booking details
        booking_query = select(
            bookings,
            rooms.c.name.label('room_name')
        ).select_from(
            bookings.join(rooms, bookings.c.room_id == rooms.c.id)
        ).where(bookings.c.id == booking_id)
        
        booking = await database.fetch_one(booking_query)
        
        if not booking:
            print(f"❌ Booking {booking_id} not found")
            return
        
        # Skip if booking is cancelled
        if booking['status'] == BookingStatus.BEKOR_QILINDI:
            print(f"⏭️  Booking {booking_id} is cancelled. Skipping notification.")
            return
        
        # Create notification message
        message = (
            f"🔔 *Bronlash eslatmasi*\n\n"
            f"📅 Sana: {booking['booking_date']}\n"
            f"⏰ Vaqt: {booking['booking_time']}\n"
            f"🏠 Xona: {booking['room_name']}\n"
            f"👤 Mijoz: {booking['customer_name']}\n"
            f"📞 Telefon: {booking['customer_phone']}\n"
            f"👥 Odamlar: {booking['guest_count']}\n"
            f"🍽 Ovqat: {booking['food_description']}\n"
            f"\n⏱ *{hours_before} soatdan keyin!*"
        )
        
        # Get all admins and cooks with telegram_chat_id
        users_query = select(
            users.c.id,
            users.c.full_name,
            users.c.telegram_chat_id,
            user_roles.c.role
        ).select_from(
            users.join(user_roles, users.c.id == user_roles.c.user_id)
        ).where(
            and_(
                users.c.telegram_chat_id.isnot(None),
                users.c.is_active == True,
                user_roles.c.role.in_([
                    UserRole.SUPERADMIN,
                    UserRole.ADMIN,
                    UserRole.OSHPAZ
                ])
            )
        )
        
        target_users = await database.fetch_all(users_query)
        
        if not target_users:
            print("⚠️  No users with Telegram configured for notifications")
            return
        
        # Send notifications to all users
        sent_count = 0
        for user in target_users:
            success = await self.send_message(
                chat_id=user['telegram_chat_id'],
                message=message
            )
            if success:
                sent_count += 1
        
        print(f"📤 Sent {sent_count}/{len(target_users)} notifications for booking {booking_id}")
    
    async def check_upcoming_bookings(self, hours_before: int = 2):
        """
        Check for upcoming bookings and send reminders.
        
        This should be called periodically (e.g., every hour) by a scheduler.
        
        Args:
            hours_before: How many hours before the booking to send reminder
        """
        now = datetime.now()
        target_time = now + timedelta(hours=hours_before)
        
        # Find bookings that are happening in 'hours_before' hours
        # and are in KUTILMOQDA or MUVAFFAQIYATLI status
        bookings_query = select(bookings.c.id).where(
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
        
        print(f"🔍 Found {len(upcoming_bookings)} upcoming bookings")
        
        for booking in upcoming_bookings:
            await self.notify_booking_reminder(booking['id'], hours_before)


# Global instance
notification_service = TelegramNotificationService()


async def send_booking_notification(booking_id: int, hours_before: int = 2):
    """
    Convenience function to send booking notification.
    
    Args:
        booking_id: ID of the booking
        hours_before: How many hours before the booking
    """
    await notification_service.notify_booking_reminder(booking_id, hours_before)


async def check_and_send_reminders():
    """
    Check for upcoming bookings and send reminders.
    This should be called by a scheduler (e.g., APScheduler, Celery, or cron).
    """
    await notification_service.check_upcoming_bookings(hours_before=2)


if __name__ == "__main__":
    # Test notification service
    async def test():
        await check_and_send_reminders()
    
    asyncio.run(test())
