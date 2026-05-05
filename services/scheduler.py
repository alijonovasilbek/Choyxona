from apscheduler.schedulers.asyncio import AsyncIOScheduler
from apscheduler.triggers.interval import IntervalTrigger
import logging
from datetime import date, timedelta, datetime, time
from sqlalchemy import select, and_, insert
from database import database
from models.main_models import bookings, rooms, users, user_roles, UserRole, notifications

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


async def check_5min_reminders():
    """Check bookings starting within 5 minutes and send FCM notifications."""
    try:
        from services.fcm_notification_service import send_fcm_to_many

        now = datetime.now()
        today = now.date()

        # Get today's bookings that haven't been notified yet
        # Since bookings only have a date (no time), we send reminders at 8:00 AM for same-day bookings
        # We'll check once a day at 8:00 AM for today's bookings
        booking_query = select(
            bookings,
            rooms.c.name.label('room_name'),
            rooms.c.filial_id.label('filial_id')
        ).select_from(
            bookings.join(rooms, bookings.c.room_id == rooms.c.id)
        ).where(
            and_(
                bookings.c.booking_date == today,
                bookings.c.status == 'KUTILMOQDA'
            )
        )

        today_bookings = await database.fetch_all(booking_query)
        if not today_bookings:
            return

        for booking in today_bookings:
            # Check if already notified today
            notif_check = select(notifications).where(
                and_(
                    notifications.c.booking_id == booking['id'],
                    notifications.c.is_sent == True
                )
            )
            existing = await database.fetch_one(notif_check)
            if existing:
                continue

            filial_id = booking['filial_id']

            # Get FCM tokens for filial users
            tokens_query = select(users.c.fcm_token, users.c.id).where(
                and_(
                    users.c.is_active == True,
                    users.c.fcm_token.isnot(None),
                    users.c.filial_id == filial_id
                )
            )
            rows = await database.fetch_all(tokens_query)
            tokens = [r['fcm_token'] for r in rows if r['fcm_token']]

            admin_tokens_query = select(users.c.fcm_token, users.c.id).select_from(
                users.join(user_roles, users.c.id == user_roles.c.user_id)
            ).where(
                and_(
                    users.c.is_active == True,
                    users.c.fcm_token.isnot(None),
                    user_roles.c.role.in_([UserRole.SUPERADMIN, UserRole.ADMIN])
                )
            )
            admin_rows = await database.fetch_all(admin_tokens_query)
            admin_tokens = [r['fcm_token'] for r in admin_rows if r['fcm_token']]

            all_tokens = list(set(tokens + admin_tokens))
            if not all_tokens:
                continue

            title = "Bugungi bron eslatmasi"
            body = f"{booking['room_name']} — bugun bron qilingan"

            sent = await send_fcm_to_many(all_tokens, title, body)
            if sent > 0:
                # Mark as notified
                await database.execute(
                    insert(notifications).values(
                        booking_id=booking['id'],
                        user_id=0,
                        message=body,
                        is_sent=True
                    )
                )
                logger.info(f"Reminder sent for booking {booking['id']} to {sent} devices")

    except Exception as e:
        logger.error(f"5min reminder check error: {e}")


class NotificationScheduler:
    def __init__(self):
        self.scheduler = AsyncIOScheduler()

    def start(self):
        # Check for today's booking reminders every morning at 08:00
        self.scheduler.add_job(
            check_5min_reminders,
            trigger='cron',
            hour=8,
            minute=0,
            id='daily_booking_reminders',
            name='Daily booking reminders at 08:00',
            replace_existing=True
        )

        self.scheduler.start()
        logger.info("Notification scheduler started (daily reminders at 08:00)")

    def shutdown(self):
        self.scheduler.shutdown()
        logger.info("Notification scheduler stopped")


scheduler = NotificationScheduler()
