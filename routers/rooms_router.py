from fastapi import APIRouter, HTTPException, status, Depends, Query
from sqlalchemy import select, insert, update, delete
from database import database
from models.main_models import rooms, filials
from schemas import RoomCreate, RoomUpdate, RoomResponse
from auth import require_admin, get_current_user
from typing import List, Optional

router = APIRouter(prefix="/rooms", tags=["Rooms Management"])


@router.post("", response_model=RoomResponse, status_code=status.HTTP_201_CREATED)
async def create_room(
        room_data: RoomCreate,
        current_user: dict = Depends(require_admin)
):
    """
    Create new room/place.
    ADMIN and SUPERADMIN can create rooms.
    Must specify which filial the room belongs to.
    """
    # Check if filial exists
    filial_query = select(filials).where(filials.c.id == room_data.filial_id)
    filial = await database.fetch_one(filial_query)
    if not filial:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Filial not found"
        )

    # Check if room name already exists in this filial
    existing_room_query = select(rooms).where(
        (rooms.c.name == room_data.name) & (rooms.c.filial_id == room_data.filial_id)
    )
    existing_room = await database.fetch_one(existing_room_query)
    if existing_room:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Room with this name already exists in this filial"
        )

    # Insert room
    insert_query = insert(rooms).values(
        name=room_data.name,
        filial_id=room_data.filial_id,
        description=room_data.description,
        is_active=True
    )
    room_id = await database.execute(insert_query)

    # Fetch created room with filial name
    room_query = select(
        rooms,
        filials.c.name.label('filial_name')
    ).select_from(
        rooms.join(filials, rooms.c.filial_id == filials.c.id)
    ).where(rooms.c.id == room_id)
    created_room = await database.fetch_one(room_query)

    return {
        "id": created_room['id'],
        "name": created_room['name'],
        "filial_id": created_room['filial_id'],
        "filial_name": created_room['filial_name'],
        "is_active": created_room['is_active'],
        "created_at": created_room['created_at'],
        "updated_at": created_room['updated_at']
    }


@router.get("", response_model=List[RoomResponse])
async def get_all_rooms(
        filial_id: Optional[int] = Query(None, description="Filter by filial"),
        include_inactive: bool = False,
        current_user: dict = Depends(get_current_user)
):
    """
    Get all rooms.
    All authenticated users can view rooms.
    Admins can see all filials, oshpaz only sees their filial.
    """
    # Base query
    query = select(
        rooms,
        filials.c.name.label('filial_name')
    ).select_from(
        rooms.join(filials, rooms.c.filial_id == filials.c.id)
    )

    # If user is oshpaz only, filter by their filial
    from models.main_models import UserRole
    if UserRole.OSHPAZ in current_user['roles'] and \
            UserRole.ADMIN not in current_user['roles'] and \
            UserRole.SUPERADMIN not in current_user['roles']:
        if current_user.get('filial_id'):
            query = query.where(rooms.c.filial_id == current_user['filial_id'])

    # Apply filters
    if filial_id:
        query = query.where(rooms.c.filial_id == filial_id)

    if not include_inactive:
        query = query.where(rooms.c.is_active == True)

    query = query.order_by(filials.c.name, rooms.c.name)
    all_rooms = await database.fetch_all(query)

    return [
        {
            "id": room['id'],
            "name": room['name'],
            "filial_id": room['filial_id'],
            "description": room['description'],
            "is_active": room['is_active'],
            "created_at": room['created_at'],
            "updated_at": room['updated_at']
        }
        for room in all_rooms
    ]


@router.get("/{room_id}", response_model=RoomResponse)
async def get_room(
        room_id: int,
        current_user: dict = Depends(get_current_user)
):
    """
    Get room by ID.
    All authenticated users can view room details.
    """
    query = select(
        rooms,
        filials.c.name.label('filial_name')
    ).select_from(
        rooms.join(filials, rooms.c.filial_id == filials.c.id)
    ).where(rooms.c.id == room_id)
    room = await database.fetch_one(query)

    if not room:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Room not found"
        )

    return {
        "id": room['id'],
        "name": room['name'],
        "filial_id": room['filial_id'],
        "description": room['description'],
        "is_active": room['is_active'],
        "created_at": room['created_at'],
        "updated_at": room['updated_at']
    }


@router.put("/{room_id}", response_model=RoomResponse)
async def update_room(
        room_id: int,
        room_data: RoomUpdate,
        current_user: dict = Depends(require_admin)
):
    """
    Update room.
    ADMIN and SUPERADMIN can update rooms.
    """
    # Check if room exists
    room_query = select(rooms).where(rooms.c.id == room_id)
    room = await database.fetch_one(room_query)

    if not room:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Room not found"
        )

    # Prepare update data
    update_data = {}
    if room_data.name is not None:
        filial_id = room_data.filial_id if room_data.filial_id else room['filial_id']
        # Check if new name is already taken by another room in the same filial
        existing_room_query = select(rooms).where(
            (rooms.c.name == room_data.name) &
            (rooms.c.filial_id == filial_id) &
            (rooms.c.id != room_id)
        )
        existing_room = await database.fetch_one(existing_room_query)
        if existing_room:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Room with this name already exists in this filial"
            )
        update_data['name'] = room_data.name

    if room_data.filial_id is not None:
        # Check if filial exists
        filial_query = select(filials).where(filials.c.id == room_data.filial_id)
        filial = await database.fetch_one(filial_query)
        if not filial:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Filial not found"
            )
        update_data['filial_id'] = room_data.filial_id

    if room_data.description is not None:
        update_data['description'] = room_data.description
    if room_data.is_active is not None:
        update_data['is_active'] = room_data.is_active

    # Update room
    if update_data:
        update_query = update(rooms).where(rooms.c.id == room_id).values(**update_data)
        await database.execute(update_query)

    # Fetch updated room
    updated_room_query = select(
        rooms,
    ).select_from(
        rooms.join(filials, rooms.c.filial_id == filials.c.id)
    ).where(rooms.c.id == room_id)
    updated_room = await database.fetch_one(updated_room_query)

    return {
        "id": updated_room['id'],
        "name": updated_room['name'],
        "filial_id": updated_room['filial_id'],
        "capacity": updated_room['capacity'],
        "is_active": updated_room['is_active'],
        "created_at": updated_room['created_at'],
        "updated_at": updated_room['updated_at']
    }


@router.delete("/{room_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_room(
        room_id: int,
        current_user: dict = Depends(require_admin)
):
    """
    Delete room.
    ADMIN and SUPERADMIN can delete rooms.
    """
    # Check if room exists
    room_query = select(rooms).where(rooms.c.id == room_id)
    room = await database.fetch_one(room_query)

    if not room:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Room not found"
        )

    # Delete room
    delete_query = delete(rooms).where(rooms.c.id == room_id)
    await database.execute(delete_query)

    return None