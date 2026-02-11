from fastapi import APIRouter, Depends, Query
from auth import get_current_user
from services.local_notification_service import notification_service
from datetime import datetime
from typing import List

router = APIRouter(prefix="/notifications", tags=["Notifications"])


@router.get("/upcoming")
async def get_upcoming_notifications(
        hours_before: int = Query(2, ge=1, le=24),
        current_user: dict = Depends(get_current_user)
):
    """
    Get upcoming bookings that need notifications.
    Android app polls this endpoint periodically.

    Args:
        hours_before: How many hours before booking (default: 2)

    Returns:
        List of upcoming bookings
    """
    bookings = await notification_service.get_upcoming_bookings(hours_before)

    return {
        "status": "success",
        "count": len(bookings),
        "bookings": bookings,
        "timestamp": datetime.now().isoformat()
    }


@router.get("/check")
async def check_notifications(
        current_user: dict = Depends(get_current_user)
):
    """
    Check if there are any upcoming bookings for notification.
    Quick endpoint for Android app to check notification status.

    Returns:
        Boolean indicating if there are notifications
    """
    bookings = await notification_service.get_upcoming_bookings(hours_before=2)

    return {
        "has_notifications": len(bookings) > 0,
        "count": len(bookings),
        "timestamp": datetime.now().isoformat()
    }