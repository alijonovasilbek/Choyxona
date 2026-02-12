from fastapi import APIRouter, HTTPException, status, Depends, Query
from sqlalchemy import select, insert, update, delete, and_, or_, func, join
from database import database
from models.main_models import bookings, rooms, users, user_roles, UserRole, BookingStatus
from schemas import (
    BookingCreate,
    BookingUpdate,
    BookingStatusUpdate,
    BookingResponse,
    BookingStatusEnum
)
from auth import require_admin, get_current_user
from typing import List, Optional
from datetime import date, datetime

router = APIRouter(prefix="/bookings", tags=["Bookings Management"])


@router.post("", response_model=BookingResponse, status_code=status.HTTP_201_CREATED)
async def create_booking(
        booking_data: BookingCreate,
        current_user: dict = Depends(require_admin)
):
    """
    Create new booking.
    Only ADMIN and SUPERADMIN can create bookings.
    Oshpaz cannot create bookings.
    """
    # Check if room exists and is active
    room_query = select(rooms).where(rooms.c.id == booking_data.room_id)
    room = await database.fetch_one(room_query)

    if not room:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Room not found"
        )

    if not room['is_active']:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Room is not active"
        )

    # Check if guest count exceeds room capacity
    if booking_data.guest_count > room['capacity']:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Guest count ({booking_data.guest_count}) exceeds room capacity ({room['capacity']})"
        )

    # Check if booking date is not in the past
    if booking_data.booking_date < date.today():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Cannot create booking for past dates"
        )

    # Insert booking - created_at va updated_at avtomatik qo'shiladi
    insert_query = insert(bookings).values(
        room_id=booking_data.room_id,
        booking_date=booking_data.booking_date,
        booking_time=booking_data.booking_time,
        customer_name=booking_data.customer_name,
        customer_phone=booking_data.customer_phone,
        guest_count=booking_data.guest_count,
        food_description=booking_data.food_description,
        description=booking_data.description,
        status=BookingStatus.KUTILMOQDA,
        created_by=current_user['id']
    )
    booking_id = await database.execute(insert_query)

    # Fetch created booking with room and user info
    booking_query = select(
        bookings,
        rooms.c.name.label('room_name'),
        users.c.full_name.label('created_by_name')
    ).select_from(
        bookings
        .join(rooms, bookings.c.room_id == rooms.c.id)
        .join(users, bookings.c.created_by == users.c.id)
    ).where(bookings.c.id == booking_id)

    created_booking = await database.fetch_one(booking_query)

    return {
        "id": created_booking['id'],
        "room_id": created_booking['room_id'],
        "room_name": created_booking['room_name'],
        "booking_date": created_booking['booking_date'],
        "booking_time": created_booking['booking_time'],
        "customer_name": created_booking['customer_name'],
        "customer_phone": created_booking['customer_phone'],
        "guest_count": created_booking['guest_count'],
        "food_description": created_booking['food_description'],
        "description": created_booking['description'],
        "status": created_booking['status'],
        "total_amount": created_booking['total_amount'],
        "cancellation_reason": created_booking['cancellation_reason'],
        "created_by": created_booking['created_by'],
        "created_by_name": created_booking['created_by_name'],
        "created_at": created_booking['created_at'],
        "updated_at": created_booking['updated_at']
    }


@router.get("", response_model=List[BookingResponse])
async def get_bookings(
        booking_date: Optional[date] = Query(None, description="Filter by date"),
        room_id: Optional[int] = Query(None, description="Filter by room"),
        status_filter: Optional[BookingStatusEnum] = Query(None, description="Filter by status"),
        current_user: dict = Depends(get_current_user)
):
    """
    Get all bookings with filters.
    All authenticated users can view bookings.
    Oshpaz can only see bookings with kutilmoqda or muvaffaqiyatli status.
    Admin and Superadmin see all bookings.
    """
    # Base query
    query = select(
        bookings,
        rooms.c.name.label('room_name'),
        users.c.full_name.label('created_by_name')
    ).select_from(
        bookings
        .join(rooms, bookings.c.room_id == rooms.c.id)
        .join(users, bookings.c.created_by == users.c.id)
    )

    # Apply filters
    conditions = []

    if booking_date:
        conditions.append(bookings.c.booking_date == booking_date)

    if room_id:
        conditions.append(bookings.c.room_id == room_id)

    if status_filter:
        conditions.append(bookings.c.status == status_filter.value)

    # If user is oshpaz, only show kutilmoqda and muvaffaqiyatli bookings
    if UserRole.OSHPAZ in current_user['roles'] and \
            UserRole.ADMIN not in current_user['roles'] and \
            UserRole.SUPERADMIN not in current_user['roles']:
        conditions.append(
            or_(
                bookings.c.status == BookingStatus.KUTILMOQDA,
                bookings.c.status == BookingStatus.MUVAFFAQIYATLI
            )
        )

    if conditions:
        query = query.where(and_(*conditions))

    query = query.order_by(bookings.c.booking_date.desc(), bookings.c.booking_time.desc())

    all_bookings = await database.fetch_all(query)

    # Hide total_amount for oshpaz
    is_oshpaz_only = (
            UserRole.OSHPAZ in current_user['roles'] and
            UserRole.ADMIN not in current_user['roles'] and
            UserRole.SUPERADMIN not in current_user['roles']
    )

    result = []
    for booking in all_bookings:
        booking_dict = {
            "id": booking['id'],
            "room_id": booking['room_id'],
            "room_name": booking['room_name'],
            "booking_date": booking['booking_date'],
            "booking_time": booking['booking_time'],
            "customer_name": booking['customer_name'],
            "customer_phone": booking['customer_phone'],
            "guest_count": booking['guest_count'],
            "food_description": booking['food_description'],
            "description": booking['description'],
            "status": booking['status'],
            "total_amount": None if is_oshpaz_only else booking['total_amount'],
            "cancellation_reason": booking['cancellation_reason'],
            "created_by": booking['created_by'],
            "created_by_name": booking['created_by_name'],
            "created_at": booking['created_at'],
            "updated_at": booking['updated_at']
        }
        result.append(booking_dict)

    return result


@router.get("/by-date/{date_value}", response_model=List[BookingResponse])
async def get_bookings_by_date(
        date_value: date,
        current_user: dict = Depends(get_current_user)
):
    """
    Get all bookings for a specific date.
    Useful for viewing daily schedule.
    """
    query = select(
        bookings,
        rooms.c.name.label('room_name'),
        users.c.full_name.label('created_by_name')
    ).select_from(
        bookings
        .join(rooms, bookings.c.room_id == rooms.c.id)
        .join(users, bookings.c.created_by == users.c.id)
    ).where(bookings.c.booking_date == date_value)

    # If user is oshpaz, only show kutilmoqda and muvaffaqiyatli bookings
    if UserRole.OSHPAZ in current_user['roles'] and \
            UserRole.ADMIN not in current_user['roles'] and \
            UserRole.SUPERADMIN not in current_user['roles']:
        query = query.where(
            or_(
                bookings.c.status == BookingStatus.KUTILMOQDA,
                bookings.c.status == BookingStatus.MUVAFFAQIYATLI
            )
        )

    query = query.order_by(bookings.c.booking_time)

    date_bookings = await database.fetch_all(query)

    # Hide total_amount for oshpaz
    is_oshpaz_only = (
            UserRole.OSHPAZ in current_user['roles'] and
            UserRole.ADMIN not in current_user['roles'] and
            UserRole.SUPERADMIN not in current_user['roles']
    )

    result = []
    for booking in date_bookings:
        booking_dict = {
            "id": booking['id'],
            "room_id": booking['room_id'],
            "room_name": booking['room_name'],
            "booking_date": booking['booking_date'],
            "booking_time": booking['booking_time'],
            "customer_name": booking['customer_name'],
            "customer_phone": booking['customer_phone'],
            "guest_count": booking['guest_count'],
            "food_description": booking['food_description'],
            "description": booking['description'],
            "status": booking['status'],
            "total_amount": None if is_oshpaz_only else booking['total_amount'],
            "cancellation_reason": booking['cancellation_reason'],
            "created_by": booking['created_by'],
            "created_by_name": booking['created_by_name'],
            "created_at": booking['created_at'],
            "updated_at": booking['updated_at']
        }
        result.append(booking_dict)

    return result


@router.get("/{booking_id}", response_model=BookingResponse)
async def get_booking(
        booking_id: int,
        current_user: dict = Depends(get_current_user)
):
    """
    Get booking by ID.
    All authenticated users can view booking details.
    """
    query = select(
        bookings,
        rooms.c.name.label('room_name'),
        users.c.full_name.label('created_by_name')
    ).select_from(
        bookings
        .join(rooms, bookings.c.room_id == rooms.c.id)
        .join(users, bookings.c.created_by == users.c.id)
    ).where(bookings.c.id == booking_id)

    booking = await database.fetch_one(query)

    if not booking:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Booking not found"
        )

    # Hide total_amount for oshpaz
    is_oshpaz_only = (
            UserRole.OSHPAZ in current_user['roles'] and
            UserRole.ADMIN not in current_user['roles'] and
            UserRole.SUPERADMIN not in current_user['roles']
    )

    return {
        "id": booking['id'],
        "room_id": booking['room_id'],
        "room_name": booking['room_name'],
        "booking_date": booking['booking_date'],
        "booking_time": booking['booking_time'],
        "customer_name": booking['customer_name'],
        "customer_phone": booking['customer_phone'],
        "guest_count": booking['guest_count'],
        "food_description": booking['food_description'],
        "description": booking['description'],
        "status": booking['status'],
        "total_amount": None if is_oshpaz_only else booking['total_amount'],
        "cancellation_reason": booking['cancellation_reason'],
        "created_by": booking['created_by'],
        "created_by_name": booking['created_by_name'],
        "created_at": booking['created_at'],
        "updated_at": booking['updated_at']
    }


@router.put("/{booking_id}", response_model=BookingResponse)
async def update_booking(
        booking_id: int,
        booking_data: BookingUpdate,
        current_user: dict = Depends(require_admin)
):
    """
    Update booking details.
    Only ADMIN and SUPERADMIN can update bookings.
    """
    # Check if booking exists
    booking_query = select(bookings).where(bookings.c.id == booking_id)
    booking = await database.fetch_one(booking_query)

    if not booking:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Booking not found"
        )

    # Prepare update data
    update_data = {}

    if booking_data.room_id is not None:
        # Check if room exists and is active
        room_query = select(rooms).where(rooms.c.id == booking_data.room_id)
        room = await database.fetch_one(room_query)

        if not room:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Room not found"
            )

        if not room['is_active']:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Room is not active"
            )

        update_data['room_id'] = booking_data.room_id

    if booking_data.booking_date is not None:
        update_data['booking_date'] = booking_data.booking_date

    if booking_data.booking_time is not None:
        update_data['booking_time'] = booking_data.booking_time

    if booking_data.customer_name is not None:
        update_data['customer_name'] = booking_data.customer_name

    if booking_data.customer_phone is not None:
        update_data['customer_phone'] = booking_data.customer_phone

    if booking_data.guest_count is not None:
        update_data['guest_count'] = booking_data.guest_count

    if booking_data.food_description is not None:
        update_data['food_description'] = booking_data.food_description

    if booking_data.description is not None:
        update_data['description'] = booking_data.description

    # Update booking - updated_at avtomatik yangilanadi
    if update_data:
        update_query = update(bookings).where(bookings.c.id == booking_id).values(**update_data)
        await database.execute(update_query)

    # Fetch updated booking
    query = select(
        bookings,
        rooms.c.name.label('room_name'),
        users.c.full_name.label('created_by_name')
    ).select_from(
        bookings
        .join(rooms, bookings.c.room_id == rooms.c.id)
        .join(users, bookings.c.created_by == users.c.id)
    ).where(bookings.c.id == booking_id)

    updated_booking = await database.fetch_one(query)

    return {
        "id": updated_booking['id'],
        "room_id": updated_booking['room_id'],
        "room_name": updated_booking['room_name'],
        "booking_date": updated_booking['booking_date'],
        "booking_time": updated_booking['booking_time'],
        "customer_name": updated_booking['customer_name'],
        "customer_phone": updated_booking['customer_phone'],
        "guest_count": updated_booking['guest_count'],
        "food_description": updated_booking['food_description'],
        "description": updated_booking['description'],
        "status": updated_booking['status'],
        "total_amount": updated_booking['total_amount'],
        "cancellation_reason": updated_booking['cancellation_reason'],
        "created_by": updated_booking['created_by'],
        "created_by_name": updated_booking['created_by_name'],
        "created_at": updated_booking['created_at'],
        "updated_at": updated_booking['updated_at']
    }


@router.patch("/{booking_id}/status", response_model=BookingResponse)
async def update_booking_status(
        booking_id: int,
        status_data: BookingStatusUpdate,
        current_user: dict = Depends(require_admin)
):
    """
    Update booking status.
    Only ADMIN and SUPERADMIN can update status.

    - For MUVAFFAQIYATLI: total_amount is required
    - For BEKOR_QILINDI: cancellation_reason is required
    - For KUTILMOQDA: no additional fields required
    """
    # Check if booking exists
    booking_query = select(bookings).where(bookings.c.id == booking_id)
    booking = await database.fetch_one(booking_query)

    if not booking:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Booking not found"
        )

    # Prepare update data
    update_data = {
        'status': status_data.status.value
    }

    # Handle status-specific fields
    if status_data.status == BookingStatusEnum.MUVAFFAQIYATLI:
        if status_data.total_amount is None:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="total_amount is required for successful bookings"
            )
        update_data['total_amount'] = status_data.total_amount
        update_data['cancellation_reason'] = None

    elif status_data.status == BookingStatusEnum.BEKOR_QILINDI:
        if not status_data.cancellation_reason:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="cancellation_reason is required for cancelled bookings"
            )
        update_data['cancellation_reason'] = status_data.cancellation_reason
        update_data['total_amount'] = None

    else:  # KUTILMOQDA
        update_data['total_amount'] = None
        update_data['cancellation_reason'] = None

    # Update booking - updated_at avtomatik yangilanadi
    update_query = update(bookings).where(bookings.c.id == booking_id).values(**update_data)
    await database.execute(update_query)

    # Fetch updated booking
    query = select(
        bookings,
        rooms.c.name.label('room_name'),
        users.c.full_name.label('created_by_name')
    ).select_from(
        bookings
        .join(rooms, bookings.c.room_id == rooms.c.id)
        .join(users, bookings.c.created_by == users.c.id)
    ).where(bookings.c.id == booking_id)

    updated_booking = await database.fetch_one(query)

    return {
        "id": updated_booking['id'],
        "room_id": updated_booking['room_id'],
        "room_name": updated_booking['room_name'],
        "booking_date": updated_booking['booking_date'],
        "booking_time": updated_booking['booking_time'],
        "customer_name": updated_booking['customer_name'],
        "customer_phone": updated_booking['customer_phone'],
        "guest_count": updated_booking['guest_count'],
        "food_description": updated_booking['food_description'],
        "description": updated_booking['description'],
        "status": updated_booking['status'],
        "total_amount": updated_booking['total_amount'],
        "cancellation_reason": updated_booking['cancellation_reason'],
        "created_by": updated_booking['created_by'],
        "created_by_name": updated_booking['created_by_name'],
        "created_at": updated_booking['created_at'],
        "updated_at": updated_booking['updated_at']
    }


@router.delete("/{booking_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_booking(
        booking_id: int,
        current_user: dict = Depends(require_admin)
):
    """
    Delete booking.
    Only ADMIN and SUPERADMIN can delete bookings.
    """
    # Check if booking exists
    booking_query = select(bookings).where(bookings.c.id == booking_id)
    booking = await database.fetch_one(booking_query)

    if not booking:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Booking not found"
        )

    # Delete booking
    delete_query = delete(bookings).where(bookings.c.id == booking_id)
    await database.execute(delete_query)

    return None