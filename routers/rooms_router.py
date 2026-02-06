from fastapi import APIRouter, HTTPException, status, Depends
from sqlalchemy import select, insert, update, delete
from database import database
from models.main_models import rooms
from schemas import RoomCreate, RoomUpdate, RoomResponse
from auth import require_superadmin, require_admin, get_current_user
from typing import List

router = APIRouter(prefix="/rooms", tags=["Rooms Management"])


@router.post("/", response_model=RoomResponse, status_code=status.HTTP_201_CREATED)
async def create_room(
        room_data: RoomCreate,
        current_user: dict = Depends(require_superadmin)
):
    """
    Create new room/place.
    Only SUPERADMIN can create rooms.
    """
    # Check if room name already exists
    existing_room_query = select(rooms).where(rooms.c.name == room_data.name)
    existing_room = await database.fetch_one(existing_room_query)
    if existing_room:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Room with this name already exists"
        )

    # Insert room - created_at va updated_at avtomatik qo'shiladi
    insert_query = insert(rooms).values(
        name=room_data.name,
        description=room_data.description,
        capacity=room_data.capacity,
        is_active=True
    )
    room_id = await database.execute(insert_query)

    # Fetch created room
    room_query = select(rooms).where(rooms.c.id == room_id)
    created_room = await database.fetch_one(room_query)

    return {
        "id": created_room['id'],
        "name": created_room['name'],
        "description": created_room['description'],
        "capacity": created_room['capacity'],
        "is_active": created_room['is_active'],
        "created_at": created_room['created_at'],
        "updated_at": created_room['updated_at']
    }


@router.get("/", response_model=List[RoomResponse])
async def get_all_rooms(
        include_inactive: bool = False,
        current_user: dict = Depends(get_current_user)
):
    """
    Get all rooms.
    All authenticated users can view rooms.
    """
    if include_inactive:
        rooms_query = select(rooms).order_by(rooms.c.name)
    else:
        rooms_query = select(rooms).where(rooms.c.is_active == True).order_by(rooms.c.name)

    all_rooms = await database.fetch_all(rooms_query)

    return [
        {
            "id": room['id'],
            "name": room['name'],
            "description": room['description'],
            "capacity": room['capacity'],
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
    room_query = select(rooms).where(rooms.c.id == room_id)
    room = await database.fetch_one(room_query)

    if not room:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Room not found"
        )

    return {
        "id": room['id'],
        "name": room['name'],
        "description": room['description'],
        "capacity": room['capacity'],
        "is_active": room['is_active'],
        "created_at": room['created_at'],
        "updated_at": room['updated_at']
    }


@router.put("/{room_id}", response_model=RoomResponse)
async def update_room(
        room_id: int,
        room_data: RoomUpdate,
        current_user: dict = Depends(require_superadmin)
):
    """
    Update room.
    Only SUPERADMIN can update rooms.
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
        # Check if new name is already taken by another room
        existing_room_query = select(rooms).where(
            (rooms.c.name == room_data.name) & (rooms.c.id != room_id)
        )
        existing_room = await database.fetch_one(existing_room_query)
        if existing_room:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Room with this name already exists"
            )
        update_data['name'] = room_data.name

    if room_data.description is not None:
        update_data['description'] = room_data.description
    if room_data.capacity is not None:
        update_data['capacity'] = room_data.capacity
    if room_data.is_active is not None:
        update_data['is_active'] = room_data.is_active

    # Update room - updated_at avtomatik yangilanadi
    if update_data:
        update_query = update(rooms).where(rooms.c.id == room_id).values(**update_data)
        await database.execute(update_query)

    # Fetch updated room
    updated_room_query = select(rooms).where(rooms.c.id == room_id)
    updated_room = await database.fetch_one(updated_room_query)

    return {
        "id": updated_room['id'],
        "name": updated_room['name'],
        "description": updated_room['description'],
        "capacity": updated_room['capacity'],
        "is_active": updated_room['is_active'],
        "created_at": updated_room['created_at'],
        "updated_at": updated_room['updated_at']
    }


@router.delete("/{room_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_room(
        room_id: int,
        current_user: dict = Depends(require_superadmin)
):
    """
    Delete room.
    Only SUPERADMIN can delete rooms.
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