from apscheduler.schedulers.asyncio import AsyncIOScheduler
from apscheduler.triggers.interval import IntervalTrigger
from services.telegram_service import check_and_send_reminders
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class NotificationScheduler:
    """Scheduler for periodic booking notification checks."""
    
    def __init__(self):
        """Initialize scheduler."""
        self.scheduler = AsyncIOScheduler()
    
    def start(self):
        """Start the scheduler."""
        # Check for upcoming bookings every hour
        self.scheduler.add_job(
            check_and_send_reminders,
            trigger=IntervalTrigger(hours=1),
            id='booking_reminders',
            name='Check and send booking reminders',
            replace_existing=True
        )
        
        self.scheduler.start()
        logger.info("📅 Notification scheduler started (checking every hour)")
    
    def shutdown(self):
        """Shutdown the scheduler."""
        self.scheduler.shutdown()
        logger.info("📅 Notification scheduler stopped")


# Global scheduler instance
scheduler = NotificationScheduler()
