from fastapi import APIRouter, HTTPException, status, Depends
from sqlalchemy import select, insert, update, delete
from database import database
from models.main_models import filials
from schemas import FilialCreate, FilialUpdate, FilialResponse
from auth import require_admin_or_oshpaz, get_current_user
from typing import List

router = APIRouter(prefix="/filials", tags=["Filials Management"])


@router.post("", response_model=FilialResponse, status_code=status.HTTP_201_CREATED)
async def create_filial(
        filial_data: FilialCreate,
        current_user: dict = Depends(require_admin_or_oshpaz)
):
    """
    Create new filial.
    Only ADMIN and SUPERADMIN can create filials.
    """
    # Check if filial name already exists
    existing_filial_query = select(filials).where(filials.c.name == filial_data.name)
    existing_filial = await database.fetch_one(existing_filial_query)
    if existing_filial:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Filial with this name already exists"
        )

    # Insert filial
    insert_query = insert(filials).values(
        name=filial_data.name,
        description=filial_data.description,
        is_active=True
    )
    filial_id = await database.execute(insert_query)

    # Fetch created filial
    filial_query = select(filials).where(filials.c.id == filial_id)
    created_filial = await database.fetch_one(filial_query)

    return {
        "id": created_filial['id'],
        "name": created_filial['name'],
        "description": created_filial['description'],
        "is_active": created_filial['is_active'],
        "created_at": created_filial['created_at'],
        "updated_at": created_filial['updated_at']
    }


@router.get("", response_model=List[FilialResponse])
async def get_all_filials(
        include_inactive: bool = False,
        current_user: dict = Depends(get_current_user)
):
    """
    Get all filials.
    All authenticated users can view filials.
    """
    query = select(filials)

    if not include_inactive:
        query = query.where(filials.c.is_active == True)

    query = query.order_by(filials.c.name)
    all_filials = await database.fetch_all(query)

    return [
        {
            "id": filial['id'],
            "name": filial['name'],
            "description": filial['description'],
            "is_active": filial['is_active'],
            "created_at": filial['created_at'],
            "updated_at": filial['updated_at']
        }
        for filial in all_filials
    ]


@router.get("/{filial_id}", response_model=FilialResponse)
async def get_filial(
        filial_id: int,
        current_user: dict = Depends(get_current_user)
):
    """
    Get filial by ID.
    All authenticated users can view filial details.
    """
    filial_query = select(filials).where(filials.c.id == filial_id)
    filial = await database.fetch_one(filial_query)

    if not filial:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Filial not found"
        )

    return {
        "id": filial['id'],
        "name": filial['name'],
        "description": filial['description'],
        "is_active": filial['is_active'],
        "created_at": filial['created_at'],
        "updated_at": filial['updated_at']
    }


@router.put("/{filial_id}", response_model=FilialResponse)
async def update_filial(
        filial_id: int,
        filial_data: FilialUpdate,
        current_user: dict = Depends(require_admin_or_oshpaz)
):
    """
    Update filial.
    Only ADMIN and SUPERADMIN can update filials.
    """
    # Check if filial exists
    filial_query = select(filials).where(filials.c.id == filial_id)
    filial = await database.fetch_one(filial_query)

    if not filial:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Filial not found"
        )

    # Prepare update data
    update_data = {}
    if filial_data.name is not None:
        # Check if new name is already taken
        existing_filial_query = select(filials).where(
            (filials.c.name == filial_data.name) & (filials.c.id != filial_id)
        )
        existing_filial = await database.fetch_one(existing_filial_query)
        if existing_filial:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Filial with this name already exists"
            )
        update_data['name'] = filial_data.name

    if filial_data.description is not None:
        update_data['description'] = filial_data.description
    if filial_data.is_active is not None:
        update_data['is_active'] = filial_data.is_active

    # Update filial
    if update_data:
        update_query = update(filials).where(filials.c.id == filial_id).values(**update_data)
        await database.execute(update_query)

    # Fetch updated filial
    updated_filial_query = select(filials).where(filials.c.id == filial_id)
    updated_filial = await database.fetch_one(updated_filial_query)

    return {
        "id": updated_filial['id'],
        "name": updated_filial['name'],
        "description": updated_filial['description'],
        "is_active": updated_filial['is_active'],
        "created_at": updated_filial['created_at'],
        "updated_at": updated_filial['updated_at']
    }


@router.delete("/{filial_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_filial(
        filial_id: int,
        current_user: dict = Depends(require_admin_or_oshpaz)
):
    """
    Delete filial.
    Only ADMIN and SUPERADMIN can delete filials.
    """
    # Check if filial exists
    filial_query = select(filials).where(filials.c.id == filial_id)
    filial = await database.fetch_one(filial_query)

    if not filial:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Filial not found"
        )

    # Delete filial
    delete_query = delete(filials).where(filials.c.id == filial_id)
    await database.execute(delete_query)

    return None