from fastapi import APIRouter, HTTPException, status, Depends, Query
from sqlalchemy import select, func, and_, or_
from database import database
from models.main_models import bookings, rooms, BookingStatus, UserRole
from schemas import BookingStatsResponse, MonthlyReportResponse
from auth import require_admin, get_current_user
from typing import Optional
from datetime import date, datetime
from calendar import monthrange

router = APIRouter(prefix="/reports", tags=["Reports & Analytics"])


@router.get("/stats", response_model=BookingStatsResponse)
async def get_booking_stats(
        date_from: Optional[date] = Query(None, description="Start date for filter"),
        date_to: Optional[date] = Query(None, description="End date for filter"),
        current_user: dict = Depends(require_admin)
):
    """
    Get booking statistics.
    Only ADMIN and SUPERADMIN can view statistics.

    Returns:
    - Total bookings count
    - Count by status (kutilmoqda, muvaffaqiyatli, bekor_qilindi)
    - Total revenue (sum of successful bookings)
    """
    # Build query conditions
    conditions = []
    if date_from:
        conditions.append(bookings.c.booking_date >= date_from)
    if date_to:
        conditions.append(bookings.c.booking_date <= date_to)

    # Get total bookings count
    total_query = select(func.count()).select_from(bookings)
    if conditions:
        total_query = total_query.where(and_(*conditions))
    total_bookings = await database.fetch_val(total_query)

    # Get count by status
    kutilmoqda_query = select(func.count()).select_from(bookings).where(
        bookings.c.status == BookingStatus.KUTILMOQDA
    )
    if conditions:
        kutilmoqda_query = kutilmoqda_query.where(and_(*conditions))
    kutilmoqda_count = await database.fetch_val(kutilmoqda_query)

    muvaffaqiyatli_query = select(func.count()).select_from(bookings).where(
        bookings.c.status == BookingStatus.MUVAFFAQIYATLI
    )
    if conditions:
        muvaffaqiyatli_query = muvaffaqiyatli_query.where(and_(*conditions))
    muvaffaqiyatli_count = await database.fetch_val(muvaffaqiyatli_query)

    bekor_qilindi_query = select(func.count()).select_from(bookings).where(
        bookings.c.status == BookingStatus.BEKOR_QILINDI
    )
    if conditions:
        bekor_qilindi_query = bekor_qilindi_query.where(and_(*conditions))
    bekor_qilindi_count = await database.fetch_val(bekor_qilindi_query)

    # Get total revenue (sum of successful bookings) - COALESCE handles NULL values
    revenue_query = select(
        func.coalesce(func.sum(bookings.c.total_amount), 0)
    ).select_from(bookings).where(
        and_(
            bookings.c.status == BookingStatus.MUVAFFAQIYATLI,
            bookings.c.total_amount.isnot(None)
        )
    )
    if conditions:
        revenue_query = revenue_query.where(and_(*conditions))
    total_revenue = await database.fetch_val(revenue_query)

    return {
        "total_bookings": total_bookings or 0,
        "kutilmoqda_count": kutilmoqda_count or 0,
        "muvaffaqiyatli_count": muvaffaqiyatli_count or 0,
        "bekor_qilindi_count": bekor_qilindi_count or 0,
        "total_revenue": float(total_revenue) if total_revenue else 0.0,
        "date_from": date_from,
        "date_to": date_to
    }


@router.get("/monthly/{year}/{month}", response_model=MonthlyReportResponse)
async def monthly_report(year: int, month: int, current_user: dict = Depends(require_admin)):
    """
    Get monthly report with breakdown by room.
    Only ADMIN and SUPERADMIN can view monthly reports.

    Returns:
    - Monthly statistics
    - Breakdown by room
    """
    # Calculate date range for the month
    first_day = date(year, month, 1)
    last_day_num = monthrange(year, month)[1]
    last_day = date(year, month, last_day_num)

    # Get total bookings for the month
    total_query = select(func.count()).select_from(bookings).where(
        and_(
            bookings.c.booking_date >= first_day,
            bookings.c.booking_date <= last_day
        )
    )
    total_bookings = await database.fetch_val(total_query)

    # Get count by status
    successful_query = select(func.count()).select_from(bookings).where(
        and_(
            bookings.c.booking_date >= first_day,
            bookings.c.booking_date <= last_day,
            bookings.c.status == BookingStatus.MUVAFFAQIYATLI
        )
    )
    successful_bookings = await database.fetch_val(successful_query)

    cancelled_query = select(func.count()).select_from(bookings).where(
        and_(
            bookings.c.booking_date >= first_day,
            bookings.c.booking_date <= last_day,
            bookings.c.status == BookingStatus.BEKOR_QILINDI
        )
    )
    cancelled_bookings = await database.fetch_val(cancelled_query)

    pending_query = select(func.count()).select_from(bookings).where(
        and_(
            bookings.c.booking_date >= first_day,
            bookings.c.booking_date <= last_day,
            bookings.c.status == BookingStatus.KUTILMOQDA
        )
    )
    pending_bookings = await database.fetch_val(pending_query)

    # Get total revenue - COALESCE handles NULL values
    revenue_query = select(
        func.coalesce(func.sum(bookings.c.total_amount), 0)
    ).select_from(bookings).where(
        and_(
            bookings.c.booking_date >= first_day,
            bookings.c.booking_date <= last_day,
            bookings.c.status == BookingStatus.MUVAFFAQIYATLI,
            bookings.c.total_amount.isnot(None)
        )
    )
    total_revenue = await database.fetch_val(revenue_query)

    # Get breakdown by room - SIMPLIFIED VERSION
    # First, get all rooms
    all_rooms_query = select(rooms.c.id, rooms.c.name).order_by(rooms.c.name)
    all_rooms = await database.fetch_all(all_rooms_query)

    by_room = []

    for room in all_rooms:
        room_id = room['id']
        room_name = room['name']

        # Total bookings for this room in the month
        room_total_query = select(func.count()).select_from(bookings).where(
            and_(
                bookings.c.room_id == room_id,
                bookings.c.booking_date >= first_day,
                bookings.c.booking_date <= last_day
            )
        )
        room_total = await database.fetch_val(room_total_query)

        # Successful bookings for this room
        room_successful_query = select(func.count()).select_from(bookings).where(
            and_(
                bookings.c.room_id == room_id,
                bookings.c.booking_date >= first_day,
                bookings.c.booking_date <= last_day,
                bookings.c.status == BookingStatus.MUVAFFAQIYATLI
            )
        )
        room_successful = await database.fetch_val(room_successful_query)

        # Revenue for this room
        room_revenue_query = select(
            func.coalesce(func.sum(bookings.c.total_amount), 0)
        ).select_from(bookings).where(
            and_(
                bookings.c.room_id == room_id,
                bookings.c.booking_date >= first_day,
                bookings.c.booking_date <= last_day,
                bookings.c.status == BookingStatus.MUVAFFAQIYATLI,
                bookings.c.total_amount.isnot(None)
            )
        )
        room_revenue = await database.fetch_val(room_revenue_query)

        by_room.append({
            "room_id": room_id,
            "room_name": room_name,
            "total_bookings": room_total or 0,
            "successful_bookings": room_successful or 0,
            "revenue": float(room_revenue) if room_revenue else 0.0
        })

    return {
        "year": year,
        "month": month,
        "total_bookings": total_bookings or 0,
        "successful_bookings": successful_bookings or 0,
        "cancelled_bookings": cancelled_bookings or 0,
        "pending_bookings": pending_bookings or 0,
        "total_revenue": float(total_revenue) if total_revenue else 0.0,
        "by_room": by_room
    }


@router.get("/daily/{date_value}")
async def get_daily_report(
        date_value: date,
        current_user: dict = Depends(require_admin)
):
    """
    Get daily report with all bookings and statistics.
    Only ADMIN and SUPERADMIN can view daily reports.
    """
    # Get all bookings for the date
    bookings_query = select(
        bookings,
        rooms.c.name.label('room_name')
    ).select_from(
        bookings.join(rooms, bookings.c.room_id == rooms.c.id)
    ).where(
        bookings.c.booking_date == date_value
    ).order_by(bookings.c.booking_time)

    day_bookings = await database.fetch_all(bookings_query)

    # Calculate statistics
    total_bookings = len(day_bookings)
    kutilmoqda_count = sum(1 for b in day_bookings if b['status'] == BookingStatus.KUTILMOQDA)
    muvaffaqiyatli_count = sum(1 for b in day_bookings if b['status'] == BookingStatus.MUVAFFAQIYATLI)
    bekor_qilindi_count = sum(1 for b in day_bookings if b['status'] == BookingStatus.BEKOR_QILINDI)

    total_revenue = sum(
        float(b['total_amount']) if b['total_amount'] else 0.0
        for b in day_bookings
        if b['status'] == BookingStatus.MUVAFFAQIYATLI
    )

    # Format bookings
    bookings_list = [
        {
            "id": b['id'],
            "room_name": b['room_name'],
            "booking_time": str(b['booking_time']),
            "customer_name": b['customer_name'],
            "customer_phone": b['customer_phone'],
            "guest_count": b['guest_count'],
            "food_description": b['food_description'],
            "status": b['status'],
            "total_amount": float(b['total_amount']) if b['total_amount'] else None
        }
        for b in day_bookings
    ]

    return {
        "date": date_value,
        "statistics": {
            "total_bookings": total_bookings,
            "kutilmoqda": kutilmoqda_count,
            "muvaffaqiyatli": muvaffaqiyatli_count,
            "bekor_qilindi": bekor_qilindi_count,
            "total_revenue": total_revenue
        },
        "bookings": bookings_list
    }