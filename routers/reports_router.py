from fastapi import APIRouter, HTTPException, status, Depends, Query
from sqlalchemy import select, func, and_, or_
from database import database
from models.main_models import bookings, rooms, filials, BookingStatus, UserRole
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
    Get booking statistics for the active filial.
    Only ADMIN and SUPERADMIN can view statistics.
    Automatically filtered by active_filial_id.

    Returns:
    - Total bookings count
    - Count by status (kutilmoqda, muvaffaqiyatli, bekor_qilindi)
    """
    active_filial_id = current_user.get('active_filial_id')

    if not active_filial_id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No active filial selected"
        )

    # Build query conditions - always filter by active filial
    conditions = [rooms.c.filial_id == active_filial_id]

    if date_from:
        conditions.append(bookings.c.booking_date >= date_from)
    if date_to:
        conditions.append(bookings.c.booking_date <= date_to)

    # Get total bookings count
    total_query = select(func.count()).select_from(
        bookings.join(rooms, bookings.c.room_id == rooms.c.id)
    ).where(and_(*conditions))
    total_bookings = await database.fetch_val(total_query)

    # Get count by status
    kutilmoqda_query = select(func.count()).select_from(
        bookings.join(rooms, bookings.c.room_id == rooms.c.id)
    ).where(
        and_(*conditions, bookings.c.status == BookingStatus.KUTILMOQDA)
    )
    kutilmoqda_count = await database.fetch_val(kutilmoqda_query)

    muvaffaqiyatli_query = select(func.count()).select_from(
        bookings.join(rooms, bookings.c.room_id == rooms.c.id)
    ).where(
        and_(*conditions, bookings.c.status == BookingStatus.MUVAFFAQIYATLI)
    )
    muvaffaqiyatli_count = await database.fetch_val(muvaffaqiyatli_query)

    bekor_qilindi_query = select(func.count()).select_from(
        bookings.join(rooms, bookings.c.room_id == rooms.c.id)
    ).where(
        and_(*conditions, bookings.c.status == BookingStatus.BEKOR_QILINDI)
    )
    bekor_qilindi_count = await database.fetch_val(bekor_qilindi_query)

    return {
        "total_bookings": total_bookings or 0,
        "kutilmoqda_count": kutilmoqda_count or 0,
        "muvaffaqiyatli_count": muvaffaqiyatli_count or 0,
        "bekor_qilindi_count": bekor_qilindi_count or 0,
        "date_from": date_from,
        "date_to": date_to
    }


@router.get("/monthly/{year}/{month}", response_model=MonthlyReportResponse)
async def monthly_report(
        year: int,
        month: int,
        current_user: dict = Depends(require_admin)
):
    """
    Get monthly report with breakdown by room for the active filial.
    Only ADMIN and SUPERADMIN can view monthly reports.
    Automatically filtered by active_filial_id.

    Returns:
    - Monthly statistics
    - Breakdown by room
    """
    active_filial_id = current_user.get('active_filial_id')

    if not active_filial_id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No active filial selected"
        )

    # Calculate date range for the month
    first_day = date(year, month, 1)
    last_day_num = monthrange(year, month)[1]
    last_day = date(year, month, last_day_num)

    # Build base conditions - always filter by active filial
    conditions = [
        bookings.c.booking_date >= first_day,
        bookings.c.booking_date <= last_day,
        rooms.c.filial_id == active_filial_id
    ]

    # Get total bookings for the month
    total_query = select(func.count()).select_from(
        bookings.join(rooms, bookings.c.room_id == rooms.c.id)
    ).where(and_(*conditions))
    total_bookings = await database.fetch_val(total_query)

    # Get count by status
    successful_query = select(func.count()).select_from(
        bookings.join(rooms, bookings.c.room_id == rooms.c.id)
    ).where(
        and_(*conditions, bookings.c.status == BookingStatus.MUVAFFAQIYATLI)
    )
    successful_bookings = await database.fetch_val(successful_query)

    cancelled_query = select(func.count()).select_from(
        bookings.join(rooms, bookings.c.room_id == rooms.c.id)
    ).where(
        and_(*conditions, bookings.c.status == BookingStatus.BEKOR_QILINDI)
    )
    cancelled_bookings = await database.fetch_val(cancelled_query)

    pending_query = select(func.count()).select_from(
        bookings.join(rooms, bookings.c.room_id == rooms.c.id)
    ).where(
        and_(*conditions, bookings.c.status == BookingStatus.KUTILMOQDA)
    )
    pending_bookings = await database.fetch_val(pending_query)

    # Get breakdown by room for the active filial
    all_rooms_query = select(
        rooms.c.id,
        rooms.c.name
    ).where(
        rooms.c.filial_id == active_filial_id
    ).order_by(rooms.c.name)
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

        by_room.append({
            "room_id": room_id,
            "room_name": room_name,
            "total_bookings": room_total or 0,
            "successful_bookings": room_successful or 0
        })

    return {
        "year": year,
        "month": month,
        "total_bookings": total_bookings or 0,
        "successful_bookings": successful_bookings or 0,
        "cancelled_bookings": cancelled_bookings or 0,
        "pending_bookings": pending_bookings or 0,
        "by_room": by_room
    }


@router.get("/daily/{date_value}")
async def get_daily_report(
        date_value: date,
        current_user: dict = Depends(require_admin)
):
    """
    Get daily report with all bookings and statistics for the active filial.
    Only ADMIN and SUPERADMIN can view daily reports.
    Automatically filtered by active_filial_id.
    """
    active_filial_id = current_user.get('active_filial_id')

    if not active_filial_id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No active filial selected"
        )

    # Build query - filter by active filial
    bookings_query = select(
        bookings,
        rooms.c.name.label('room_name'),
        filials.c.name.label('filial_name')
    ).select_from(
        bookings.join(rooms, bookings.c.room_id == rooms.c.id)
        .join(filials, rooms.c.filial_id == filials.c.id)
    ).where(
        and_(
            bookings.c.booking_date == date_value,
            rooms.c.filial_id == active_filial_id
        )
    ).order_by(rooms.c.name)

    day_bookings = await database.fetch_all(bookings_query)

    # Calculate statistics
    total_bookings = len(day_bookings)
    kutilmoqda_count = sum(1 for b in day_bookings if b['status'] == BookingStatus.KUTILMOQDA)
    muvaffaqiyatli_count = sum(1 for b in day_bookings if b['status'] == BookingStatus.MUVAFFAQIYATLI)
    bekor_qilindi_count = sum(1 for b in day_bookings if b['status'] == BookingStatus.BEKOR_QILINDI)

    # Format bookings
    bookings_list = [
        {
            "id": b['id'],
            "room_name": b['room_name'],
            "filial_name": b['filial_name'],
            "description": b['description'],
            "status": b['status']
        }
        for b in day_bookings
    ]

    return {
        "date": date_value,
        "statistics": {
            "total_bookings": total_bookings,
            "kutilmoqda": kutilmoqda_count,
            "muvaffaqiyatli": muvaffaqiyatli_count,
            "bekor_qilindi": bekor_qilindi_count
        },
        "bookings": bookings_list
    }