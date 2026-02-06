from fastapi import APIRouter, HTTPException, status, Depends
from auth import require_admin
from services.telegram_service import send_booking_notification, check_and_send_reminders

router = APIRouter(prefix="/notifications", tags=["Notifications"])


@router.post("/test/{booking_id}")
async def test_booking_notification(
    booking_id: int,
    current_user: dict = Depends(require_admin)
):
    """
    Test notification for a specific booking.
    Only ADMIN and SUPERADMIN can trigger test notifications.
    
    This will send a notification to all admins and cooks about the booking.
    """
    try:
        await send_booking_notification(booking_id, hours_before=2)
        return {
            "status": "success",
            "message": f"Test notification sent for booking {booking_id}"
        }
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to send notification: {str(e)}"
        )


@router.post("/check-reminders")
async def trigger_reminder_check(
    current_user: dict = Depends(require_admin)
):
    """
    Manually trigger reminder check.
    Only ADMIN and SUPERADMIN can trigger reminder checks.
    
    This will check all bookings that are 2 hours away and send notifications.
    """
    try:
        await check_and_send_reminders()
        return {
            "status": "success",
            "message": "Reminder check completed"
        }
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to check reminders: {str(e)}"
        )
