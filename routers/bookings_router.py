from fastapi import APIRouter, HTTPException, status, Depends, Query
from sqlalchemy import select, insert, update, delete, and_, or_, func, join
from database import database
from models.main_models import bookings, rooms, users, user_roles, UserRole, BookingStatus, filials
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

    Admin/Superadmin: Can create booking for any room (from any filial)
    One room can only be booked once per day.
    """
    # Check if room exists
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

    # Check if booking date is not in the past
    if booking_data.booking_date < date.today():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Cannot create booking for past dates"
        )

    # Check if room is already booked for this date
    existing_booking_query = select(bookings).where(
        and_(
            bookings.c.room_id == booking_data.room_id,
            bookings.c.booking_date == booking_data.booking_date
        )
    )
    existing_booking = await database.fetch_one(existing_booking_query)
    if existing_booking:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Room is already booked for this date"
        )

    # Insert booking
    insert_query = insert(bookings).values(
        room_id=booking_data.room_id,
        booking_date=booking_data.booking_date,
        description=booking_data.description,
        status=BookingStatus.KUTILMOQDA,
        created_by=current_user['id']
    )
    booking_id = await database.execute(insert_query)

    # Fetch created booking with room, filial and user info
    booking_query = select(
        bookings,
        rooms.c.name.label('room_name'),
        rooms.c.filial_id.label('filial_id'),
        filials.c.name.label('filial_name'),
        users.c.full_name.label('created_by_name')
    ).select_from(
        bookings
        .join(rooms, bookings.c.room_id == rooms.c.id)
        .join(filials, rooms.c.filial_id == filials.c.id)
        .join(users, bookings.c.created_by == users.c.id)
    ).where(bookings.c.id == booking_id)

    created_booking = await database.fetch_one(booking_query)

    return {
        "id": created_booking['id'],
        "room_id": created_booking['room_id'],
        "room_name": created_booking['room_name'],
        "filial_id": created_booking['filial_id'],
        "filial_name": created_booking['filial_name'],
        "booking_date": created_booking['booking_date'],
        "description": created_booking['description'],
        "status": created_booking['status'],
        "created_by": created_booking['created_by'],
        "created_by_name": created_booking['created_by_name'],
        "created_at": created_booking['created_at'],
        "updated_at": created_booking['updated_at']
    }


@router.get("", response_model=List[BookingResponse])
async def get_bookings(
        booking_date: Optional[date] = Query(None, description="Filter by date"),
        room_id: Optional[int] = Query(None, description="Filter by room"),
        filial_id: Optional[int] = Query(None, description="Filter by filial (Admin/Superadmin only)"),
        status_filter: Optional[BookingStatusEnum] = Query(None, description="Filter by status"),
        current_user: dict = Depends(get_current_user)
):
    """
    Get all bookings.

    Admin/Superadmin: Can see bookings from all filials (can filter by filial_id)
    Oshpaz: Can only see bookings from their assigned filial (only KUTILMOQDA and MUVAFFAQIYATLI)
    """
    # Determine user type
    is_oshpaz_only = (
            UserRole.OSHPAZ in current_user['roles'] and
            UserRole.ADMIN not in current_user['roles'] and
            UserRole.SUPERADMIN not in current_user['roles']
    )

    # Base query
    query = select(
        bookings,
        rooms.c.name.label('room_name'),
        rooms.c.filial_id.label('filial_id'),
        filials.c.name.label('filial_name'),
        users.c.full_name.label('created_by_name')
    ).select_from(
        bookings
        .join(rooms, bookings.c.room_id == rooms.c.id)
        .join(filials, rooms.c.filial_id == filials.c.id)
        .join(users, bookings.c.created_by == users.c.id)
    )

    # Apply filters
    conditions = []

    # Filial filter
    if is_oshpaz_only:
        # Oshpaz: only their filial
        if not current_user.get('active_filial_id'):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Oshpaz must be assigned to a filial"
            )
        conditions.append(rooms.c.filial_id == current_user['active_filial_id'])

        # Oshpaz: only KUTILMOQDA and MUVAFFAQIYATLI
        conditions.append(
            or_(
                bookings.c.status == BookingStatus.KUTILMOQDA,
                bookings.c.status == BookingStatus.MUVAFFAQIYATLI
            )
        )
    else:
        # Admin/Superadmin: can filter by filial_id if provided
        if filial_id:
            conditions.append(rooms.c.filial_id == filial_id)

    if booking_date:
        conditions.append(bookings.c.booking_date == booking_date)

    if room_id:
        conditions.append(bookings.c.room_id == room_id)

    if status_filter:
        conditions.append(bookings.c.status == status_filter.value)

    if conditions:
        query = query.where(and_(*conditions))

    query = query.order_by(bookings.c.booking_date.desc())

    all_bookings = await database.fetch_all(query)

    return [
        {
            "id": booking['id'],
            "room_id": booking['room_id'],
            "room_name": booking['room_name'],
            "filial_id": booking['filial_id'],
            "filial_name": booking['filial_name'],
            "booking_date": booking['booking_date'],
            "description": booking['description'],
            "status": booking['status'],
            "created_by": booking['created_by'],
            "created_by_name": booking['created_by_name'],
            "created_at": booking['created_at'],
            "updated_at": booking['updated_at']
        }
        for booking in all_bookings
    ]


@router.get("/by-date/{date_value}", response_model=List[BookingResponse])
async def get_bookings_by_date(
        date_value: date,
        current_user: dict = Depends(get_current_user)
):
    """
    Get all bookings for a specific date in the active filial.
    Useful for viewing daily schedule.
    """
    active_filial_id = current_user.get('active_filial_id')

    if not active_filial_id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No active filial selected"
        )

    query = select(
        bookings,
        rooms.c.name.label('room_name'),
        rooms.c.filial_id.label('filial_id'),
        filials.c.name.label('filial_name'),
        users.c.full_name.label('created_by_name')
    ).select_from(
        bookings
        .join(rooms, bookings.c.room_id == rooms.c.id)
        .join(filials, rooms.c.filial_id == filials.c.id)
        .join(users, bookings.c.created_by == users.c.id)
    ).where(
        and_(
            bookings.c.booking_date == date_value,
            rooms.c.filial_id == active_filial_id
        )
    )

    # If user is oshpaz, only show pending and successful bookings
    is_oshpaz_only = (
            UserRole.OSHPAZ in current_user['roles'] and
            UserRole.ADMIN not in current_user['roles'] and
            UserRole.SUPERADMIN not in current_user['roles']
    )

    if is_oshpaz_only:
        query = query.where(
            or_(
                bookings.c.status == BookingStatus.KUTILMOQDA,
                bookings.c.status == BookingStatus.MUVAFFAQIYATLI
            )
        )

    query = query.order_by(rooms.c.name)
    date_bookings = await database.fetch_all(query)

    return [
        {
            "id": booking['id'],
            "room_id": booking['room_id'],
            "room_name": booking['room_name'],
            "filial_id": booking['filial_id'],
            "filial_name": booking['filial_name'],
            "booking_date": booking['booking_date'],
            "description": booking['description'],
            "status": booking['status'],
            "created_by": booking['created_by'],
            "created_by_name": booking['created_by_name'],
            "created_at": booking['created_at'],
            "updated_at": booking['updated_at']
        }
        for booking in date_bookings
    ]


@router.get("/{booking_id}", response_model=BookingResponse)
async def get_booking(
        booking_id: int,
        current_user: dict = Depends(get_current_user)
):
    """
    Get booking by ID.
    All authenticated users can view booking details from their filial.
    """
    active_filial_id = current_user.get('active_filial_id')

    if not active_filial_id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No active filial selected"
        )

    query = select(
        bookings,
        rooms.c.name.label('room_name'),
        rooms.c.filial_id.label('filial_id'),
        filials.c.name.label('filial_name'),
        users.c.full_name.label('created_by_name')
    ).select_from(
        bookings
        .join(rooms, bookings.c.room_id == rooms.c.id)
        .join(filials, rooms.c.filial_id == filials.c.id)
        .join(users, bookings.c.created_by == users.c.id)
    ).where(bookings.c.id == booking_id)

    booking = await database.fetch_one(query)

    if not booking:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Booking not found"
        )

    # Check if booking belongs to user's active filial
    if booking['filial_id'] != active_filial_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Booking does not belong to your active filial"
        )

    return {
        "id": booking['id'],
        "room_id": booking['room_id'],
        "room_name": booking['room_name'],
        "filial_id": booking['filial_id'],
        "filial_name": booking['filial_name'],
        "booking_date": booking['booking_date'],
        "description": booking['description'],
        "status": booking['status'],
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
    Booking must belong to active filial.
    """
    active_filial_id = current_user.get('active_filial_id')

    if not active_filial_id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No active filial selected"
        )

    # Check if booking exists and belongs to active filial
    booking_query = select(
        bookings,
        rooms.c.filial_id
    ).select_from(
        bookings.join(rooms, bookings.c.room_id == rooms.c.id)
    ).where(bookings.c.id == booking_id)

    booking = await database.fetch_one(booking_query)

    if not booking:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Booking not found"
        )

    if booking['filial_id'] != active_filial_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Booking does not belong to your active filial"
        )

    # Prepare update data
    update_data = {}

    if booking_data.room_id is not None:
        # Check if new room exists and belongs to active filial
        room_query = select(rooms).where(rooms.c.id == booking_data.room_id)
        room = await database.fetch_one(room_query)

        if not room:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Room not found"
            )

        if room['filial_id'] != active_filial_id:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Room does not belong to your active filial"
            )

        if not room['is_active']:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Room is not active"
            )

        # Check for duplicate booking
        booking_date_to_check = booking_data.booking_date if booking_data.booking_date else booking['booking_date']
        if booking_data.room_id != booking['room_id'] or \
                (booking_data.booking_date and booking_data.booking_date != booking['booking_date']):
            existing_booking_query = select(bookings).where(
                and_(
                    bookings.c.room_id == booking_data.room_id,
                    bookings.c.booking_date == booking_date_to_check,
                    bookings.c.id != booking_id
                )
            )
            existing_booking = await database.fetch_one(existing_booking_query)
            if existing_booking:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Room is already booked for this date"
                )

        update_data['room_id'] = booking_data.room_id

    if booking_data.booking_date is not None:
        # Check for duplicate booking
        room_id_to_check = booking_data.room_id if booking_data.room_id else booking['room_id']
        if booking_data.booking_date != booking['booking_date']:
            existing_booking_query = select(bookings).where(
                and_(
                    bookings.c.room_id == room_id_to_check,
                    bookings.c.booking_date == booking_data.booking_date,
                    bookings.c.id != booking_id
                )
            )
            existing_booking = await database.fetch_one(existing_booking_query)
            if existing_booking:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Room is already booked for this date"
                )
        update_data['booking_date'] = booking_data.booking_date

    if booking_data.description is not None:
        update_data['description'] = booking_data.description

    # Update booking
    if update_data:
        update_query = update(bookings).where(bookings.c.id == booking_id).values(**update_data)
        await database.execute(update_query)

    # Fetch updated booking
    query = select(
        bookings,
        rooms.c.name.label('room_name'),
        rooms.c.filial_id.label('filial_id'),
        filials.c.name.label('filial_name'),
        users.c.full_name.label('created_by_name')
    ).select_from(
        bookings
        .join(rooms, bookings.c.room_id == rooms.c.id)
        .join(filials, rooms.c.filial_id == filials.c.id)
        .join(users, bookings.c.created_by == users.c.id)
    ).where(bookings.c.id == booking_id)

    updated_booking = await database.fetch_one(query)

    return {
        "id": updated_booking['id'],
        "room_id": updated_booking['room_id'],
        "room_name": updated_booking['room_name'],
        "filial_id": updated_booking['filial_id'],
        "filial_name": updated_booking['filial_name'],
        "booking_date": updated_booking['booking_date'],
        "description": updated_booking['description'],
        "status": updated_booking['status'],
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
    Booking must belong to active filial.
    """
    active_filial_id = current_user.get('active_filial_id')

    if not active_filial_id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No active filial selected"
        )

    # Check if booking exists and belongs to active filial
    booking_query = select(
        bookings,
        rooms.c.filial_id
    ).select_from(
        bookings.join(rooms, bookings.c.room_id == rooms.c.id)
    ).where(bookings.c.id == booking_id)

    booking = await database.fetch_one(booking_query)

    if not booking:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Booking not found"
        )

    if booking['filial_id'] != active_filial_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Booking does not belong to your active filial"
        )

    # Update booking status
    update_data = {'status': status_data.status.value}
    update_query = update(bookings).where(bookings.c.id == booking_id).values(**update_data)
    await database.execute(update_query)

    # Fetch updated booking
    query = select(
        bookings,
        rooms.c.name.label('room_name'),
        rooms.c.filial_id.label('filial_id'),
        filials.c.name.label('filial_name'),
        users.c.full_name.label('created_by_name')
    ).select_from(
        bookings
        .join(rooms, bookings.c.room_id == rooms.c.id)
        .join(filials, rooms.c.filial_id == filials.c.id)
        .join(users, bookings.c.created_by == users.c.id)
    ).where(bookings.c.id == booking_id)

    updated_booking = await database.fetch_one(query)

    return {
        "id": updated_booking['id'],
        "room_id": updated_booking['room_id'],
        "room_name": updated_booking['room_name'],
        "filial_id": updated_booking['filial_id'],
        "filial_name": updated_booking['filial_name'],
        "booking_date": updated_booking['booking_date'],
        "description": updated_booking['description'],
        "status": updated_booking['status'],
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
    Booking must belong to active filial.
    """
    active_filial_id = current_user.get('active_filial_id')

    if not active_filial_id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No active filial selected"
        )

    # Check if booking exists and belongs to active filial
    booking_query = select(
        bookings,
        rooms.c.filial_id
    ).select_from(
        bookings.join(rooms, bookings.c.room_id == rooms.c.id)
    ).where(bookings.c.id == booking_id)

    booking = await database.fetch_one(booking_query)

    if not booking:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Booking not found"
        )

    if booking['filial_id'] != active_filial_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Booking does not belong to your active filial"
        )

    # Delete booking
    delete_query = delete(bookings).where(bookings.c.id == booking_id)
    await database.execute(delete_query)

    return None