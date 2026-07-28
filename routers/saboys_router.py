from datetime import date
from typing import List, Optional

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import delete, insert, select, update

from auth import get_current_user, require_admin_or_oshpaz
from database import database
from models.main_models import UserRole, filials, saboys, users
from schemas import SaboyCreate, SaboyResponse, SaboyUpdate

router = APIRouter(prefix="/saboys", tags=["Saboys Management"])


def serialize_saboy(saboy: dict) -> dict:
    return {
        "id": saboy["id"],
        "filial_id": saboy["filial_id"],
        "filial_name": saboy["filial_name"],
        "saboy_date": saboy["saboy_date"],
        "saboy_time": saboy["saboy_time"],
        "description": saboy["description"],
        "created_by": saboy["created_by"],
        "created_by_name": saboy["created_by_name"],
        "created_at": saboy["created_at"],
        "updated_at": saboy["updated_at"],
    }


def build_saboy_query():
    return select(
        saboys,
        filials.c.name.label("filial_name"),
        users.c.full_name.label("created_by_name")
    ).select_from(
        saboys
        .join(filials, saboys.c.filial_id == filials.c.id)
        .join(users, saboys.c.created_by == users.c.id)
    )


def validate_active_filial_access(current_user: dict, filial_id: int):
    active_filial_id = current_user.get("active_filial_id")
    if active_filial_id and filial_id != active_filial_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Saboy does not belong to your active filial"
        )


@router.post("", response_model=SaboyResponse, status_code=status.HTTP_201_CREATED)
async def create_saboy(
    saboy_data: SaboyCreate,
    current_user: dict = Depends(require_admin_or_oshpaz)
):
    filial_query = select(filials).where(filials.c.id == saboy_data.filial_id)
    filial = await database.fetch_one(filial_query)

    if not filial:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Filial not found"
        )

    if not filial['has_saboy']:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Bu filialda saboy yuritilmaydi"
        )

    validate_active_filial_access(current_user, saboy_data.filial_id)

    insert_query = insert(saboys).values(
        filial_id=saboy_data.filial_id,
        saboy_date=saboy_data.saboy_date,
        saboy_time=saboy_data.saboy_time,
        description=saboy_data.description,
        created_by=current_user["id"]
    )
    saboy_id = await database.execute(insert_query)

    saboy_query = build_saboy_query().where(saboys.c.id == saboy_id)

    created_saboy = await database.fetch_one(saboy_query)
    return serialize_saboy(created_saboy)


@router.get("", response_model=List[SaboyResponse])
async def get_saboys(
    saboy_date: Optional[date] = Query(None, description="Filter by date"),
    filial_id: Optional[int] = Query(None, description="Filter by filial"),
    current_user: dict = Depends(get_current_user)
):
    is_oshpaz_only = (
        UserRole.OSHPAZ in current_user["roles"] and
        UserRole.ADMIN not in current_user["roles"] and
        UserRole.SUPERADMIN not in current_user["roles"]
    )

    query = build_saboy_query()

    if is_oshpaz_only:
        active_filial_id = current_user.get("active_filial_id")
        if not active_filial_id:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Oshpaz must be assigned to a filial"
            )
        query = query.where(saboys.c.filial_id == active_filial_id)
    elif filial_id:
        query = query.where(saboys.c.filial_id == filial_id)

    if saboy_date:
        query = query.where(saboys.c.saboy_date == saboy_date)

    query = query.order_by(
        saboys.c.saboy_date,
        saboys.c.saboy_time,
        saboys.c.created_at,
        saboys.c.id
    )

    all_saboys = await database.fetch_all(query)
    return [serialize_saboy(saboy) for saboy in all_saboys]


@router.get("/{saboy_id}", response_model=SaboyResponse)
async def get_saboy(
    saboy_id: int,
    current_user: dict = Depends(get_current_user)
):
    query = build_saboy_query().where(saboys.c.id == saboy_id)

    saboy = await database.fetch_one(query)
    if not saboy:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Saboy not found"
        )

    is_oshpaz_only = (
        UserRole.OSHPAZ in current_user["roles"] and
        UserRole.ADMIN not in current_user["roles"] and
        UserRole.SUPERADMIN not in current_user["roles"]
    )

    if is_oshpaz_only and saboy["filial_id"] != current_user.get("active_filial_id"):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Saboy does not belong to your filial"
        )

    return serialize_saboy(saboy)


@router.put("/{saboy_id}", response_model=SaboyResponse)
async def update_saboy(
    saboy_id: int,
    saboy_data: SaboyUpdate,
    current_user: dict = Depends(require_admin_or_oshpaz)
):
    query = build_saboy_query().where(saboys.c.id == saboy_id)
    saboy = await database.fetch_one(query)

    if not saboy:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Saboy not found"
        )

    validate_active_filial_access(current_user, saboy["filial_id"])

    update_data = {}

    if saboy_data.filial_id is not None:
        filial_query = select(filials).where(filials.c.id == saboy_data.filial_id)
        filial = await database.fetch_one(filial_query)
        if not filial:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Filial not found"
            )
        validate_active_filial_access(current_user, saboy_data.filial_id)
        update_data["filial_id"] = saboy_data.filial_id

    if saboy_data.saboy_date is not None:
        update_data["saboy_date"] = saboy_data.saboy_date

    if saboy_data.saboy_time is not None:
        update_data["saboy_time"] = saboy_data.saboy_time

    if saboy_data.description is not None:
        update_data["description"] = saboy_data.description

    if update_data:
        update_query = update(saboys).where(saboys.c.id == saboy_id).values(**update_data)
        await database.execute(update_query)

    updated_saboy = await database.fetch_one(build_saboy_query().where(saboys.c.id == saboy_id))
    return serialize_saboy(updated_saboy)


@router.delete("/{saboy_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_saboy(
    saboy_id: int,
    current_user: dict = Depends(require_admin_or_oshpaz)
):
    query = select(saboys).where(saboys.c.id == saboy_id)
    saboy = await database.fetch_one(query)

    if not saboy:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Saboy not found"
        )

    validate_active_filial_access(current_user, saboy["filial_id"])

    delete_query = delete(saboys).where(saboys.c.id == saboy_id)
    await database.execute(delete_query)

    return None
