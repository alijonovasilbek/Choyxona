from fastapi import APIRouter, HTTPException, status, Depends, Query
from sqlalchemy import select, func, and_, extract, case
from database import database
from models.main_models import bookings, rooms, BookingStatus, UserRole
from schemas import BookingStatsResponse, MonthlyReportResponse
from auth import require_admin, get_current_user
from typing import Optional
from datetime import date, datetime

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

    # Get total revenue (sum of successful bookings)
    revenue_query = select(func.sum(bookings.c.total_amount)).select_from(bookings).where(
        bookings.c.status == BookingStatus.MUVAFFAQIYATLI
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
    # Get total bookings for the month
    total_query = select(func.count()).select_from(bookings).where(
        and_(
            extract('year', bookings.c.booking_date) == year,
            extract('month', bookings.c.booking_date) == month
        )
    )
    total_bookings = await database.fetch_val(total_query)

    # Get count by status
    successful_query = select(func.count()).select_from(bookings).where(
        and_(
            extract('year', bookings.c.booking_date) == year,
            extract('month', bookings.c.booking_date) == month,
            bookings.c.status == BookingStatus.MUVAFFAQIYATLI
        )
    )
    successful_bookings = await database.fetch_val(successful_query)

    cancelled_query = select(func.count()).select_from(bookings).where(
        and_(
            extract('year', bookings.c.booking_date) == year,
            extract('month', bookings.c.booking_date) == month,
            bookings.c.status == BookingStatus.BEKOR_QILINDI
        )
    )
    cancelled_bookings = await database.fetch_val(cancelled_query)

    pending_query = select(func.count()).select_from(bookings).where(
        and_(
            extract('year', bookings.c.booking_date) == year,
            extract('month', bookings.c.booking_date) == month,
            bookings.c.status == BookingStatus.KUTILMOQDA
        )
    )
    pending_bookings = await database.fetch_val(pending_query)

    # Get total revenue
    revenue_query = select(func.sum(bookings.c.total_amount)).select_from(bookings).where(
        and_(
            extract('year', bookings.c.booking_date) == year,
            extract('month', bookings.c.booking_date) == month,
            bookings.c.status == BookingStatus.MUVAFFAQIYATLI
        )
    )
    total_revenue = await database.fetch_val(revenue_query)

    # Get breakdown by room - TUZATILGAN case sintaksisi
    room_query = select(
        rooms.c.id,
        rooms.c.name,
        func.count(bookings.c.id).label('total_bookings'),
        func.sum(
            case(
                (bookings.c.status == BookingStatus.MUVAFFAQIYATLI, 1),
                else_=0
            )
        ).label('successful_count'),
        func.sum(
            case(
                (bookings.c.status == BookingStatus.MUVAFFAQIYATLI, bookings.c.total_amount),
                else_=0
            )
        ).label('revenue')
    ).select_from(
        rooms.outerjoin(
            bookings,
            and_(
                rooms.c.id == bookings.c.room_id,
                extract('year', bookings.c.booking_date) == year,
                extract('month', bookings.c.booking_date) == month
            )
        )
    ).group_by(rooms.c.id, rooms.c.name).order_by(rooms.c.name)

    room_data = await database.fetch_all(room_query)

    by_room = [
        {
            "room_id": row['id'],
            "room_name": row['name'],
            "total_bookings": row['total_bookings'] or 0,
            "successful_bookings": int(row['successful_count']) if row['successful_count'] else 0,
            "revenue": float(row['revenue']) if row['revenue'] else 0.0
        }
        for row in room_data
    ]

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