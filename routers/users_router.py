from fastapi import APIRouter, HTTPException, status, Depends
from sqlalchemy import select, insert, update, delete, and_
from database import database
from models.main_models import users, user_roles, filials, UserRole
from schemas import UserCreate, UserUpdate, UserResponse
from auth import (
    get_password_hash,
    require_admin,
    get_current_user
)
from typing import List

router = APIRouter(prefix="/users", tags=["User Management"])


@router.post("", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
async def create_user(
        user_data: UserCreate,
        current_user: dict = Depends(require_admin)
):
    """
    Create new user (Admin or Oshpaz).
    ADMIN and SUPERADMIN can create users.

    - Oshpaz must have filial_id
    - Admin/Superadmin don't need filial_id
    """
    # Check if username exists
    existing_user_query = select(users).where(users.c.username == user_data.username)
    existing_user = await database.fetch_one(existing_user_query)
    if existing_user:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Username already registered"
        )

    # Check if phone exists
    existing_phone_query = select(users).where(users.c.phone == user_data.phone)
    existing_phone = await database.fetch_one(existing_phone_query)
    if existing_phone:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Phone number already registered"
        )

    # Check if user is Oshpaz only
    is_oshpaz_only = (
            UserRole.OSHPAZ in user_data.roles and
            UserRole.ADMIN not in user_data.roles and
            UserRole.SUPERADMIN not in user_data.roles
    )

    # Oshpaz must have filial_id
    if is_oshpaz_only and not user_data.filial_id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Oshpaz must be assigned to a filial"
        )

    # If filial_id is provided, check if it exists
    if user_data.filial_id:
        filial_query = select(filials).where(filials.c.id == user_data.filial_id)
        filial = await database.fetch_one(filial_query)
        if not filial:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Filial not found"
            )

    # Hash password
    hashed_password = get_password_hash(user_data.password)

    # Insert user
    insert_query = insert(users).values(
        full_name=user_data.full_name,
        phone=user_data.phone,
        username=user_data.username,
        password_hash=hashed_password,
        filial_id=user_data.filial_id,
        is_active=True
    )
    user_id = await database.execute(insert_query)

    # Assign roles
    for role in user_data.roles:
        role_insert = insert(user_roles).values(
            user_id=user_id,
            role=role
        )
        await database.execute(role_insert)

    # Fetch created user
    user_query = select(users).where(users.c.id == user_id)
    created_user = await database.fetch_one(user_query)

    # Get filial name if assigned
    filial_name = None
    if created_user['filial_id']:
        filial_query = select(filials.c.name).where(filials.c.id == created_user['filial_id'])
        filial = await database.fetch_one(filial_query)
        if filial:
            filial_name = filial['name']

    return {
        "id": created_user['id'],
        "full_name": created_user['full_name'],
        "phone": created_user['phone'],
        "username": created_user['username'],
        "filial_id": created_user['filial_id'],
        "filial_name": filial_name,
        "is_active": created_user['is_active'],
        "roles": user_data.roles,
        "created_at": created_user['created_at']
    }


@router.get("", response_model=List[UserResponse])
async def get_all_users(
        current_user: dict = Depends(require_admin)
):
    """
    Get all users.
    ADMIN and SUPERADMIN can view all users.
    """
    # Get all users
    users_query = select(users).order_by(users.c.created_at.desc())
    all_users = await database.fetch_all(users_query)

    result = []
    for user in all_users:
        # Get user roles
        roles_query = select(user_roles.c.role).where(user_roles.c.user_id == user['id'])
        roles_result = await database.fetch_all(roles_query)
        user_roles_list = [role['role'] for role in roles_result]

        # Get filial name if assigned
        filial_name = None
        if user['filial_id']:
            filial_query = select(filials.c.name).where(filials.c.id == user['filial_id'])
            filial = await database.fetch_one(filial_query)
            if filial:
                filial_name = filial['name']

        result.append({
            "id": user['id'],
            "full_name": user['full_name'],
            "phone": user['phone'],
            "username": user['username'],
            "filial_id": user['filial_id'],
            "filial_name": filial_name,
            "is_active": user['is_active'],
            "roles": user_roles_list,
            "created_at": user['created_at']
        })

    return result


@router.get("/{user_id}", response_model=UserResponse)
async def get_user(
        user_id: int,
        current_user: dict = Depends(require_admin)
):
    """
    Get user by ID.
    ADMIN and SUPERADMIN can view user details.
    """
    user_query = select(users).where(users.c.id == user_id)
    user = await database.fetch_one(user_query)

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User not found"
        )

    # Get user roles
    roles_query = select(user_roles.c.role).where(user_roles.c.user_id == user_id)
    roles_result = await database.fetch_all(roles_query)
    user_roles_list = [role['role'] for role in roles_result]

    # Get filial name if assigned
    filial_name = None
    if user['filial_id']:
        filial_query = select(filials.c.name).where(filials.c.id == user['filial_id'])
        filial = await database.fetch_one(filial_query)
        if filial:
            filial_name = filial['name']

    return {
        "id": user['id'],
        "full_name": user['full_name'],
        "phone": user['phone'],
        "username": user['username'],
        "filial_id": user['filial_id'],
        "filial_name": filial_name,
        "is_active": user['is_active'],
        "roles": user_roles_list,
        "created_at": user['created_at']
    }


@router.put("/{user_id}", response_model=UserResponse)
async def update_user(
        user_id: int,
        user_data: UserUpdate,
        current_user: dict = Depends(require_admin)
):
    """
    Update user.
    ADMIN and SUPERADMIN can update users.
    """
    # Check if user exists
    user_query = select(users).where(users.c.id == user_id)
    user = await database.fetch_one(user_query)

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User not found"
        )

    # Prepare update data
    update_data = {}
    if user_data.full_name is not None:
        update_data['full_name'] = user_data.full_name
    if user_data.phone is not None:
        # Check if phone is already taken by another user
        existing_phone_query = select(users).where(
            and_(users.c.phone == user_data.phone, users.c.id != user_id)
        )
        existing_phone = await database.fetch_one(existing_phone_query)
        if existing_phone:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Phone number already registered"
            )
        update_data['phone'] = user_data.phone

    if user_data.filial_id is not None:
        # Check if filial exists
        if user_data.filial_id:
            filial_query = select(filials).where(filials.c.id == user_data.filial_id)
            filial = await database.fetch_one(filial_query)
            if not filial:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="Filial not found"
                )
        update_data['filial_id'] = user_data.filial_id

    if user_data.is_active is not None:
        update_data['is_active'] = user_data.is_active

    # Update user
    if update_data:
        update_query = update(users).where(users.c.id == user_id).values(**update_data)
        await database.execute(update_query)

    # Update roles if provided
    if user_data.roles is not None:
        # Check if Oshpaz only and has filial_id
        is_oshpaz_only = (
                UserRole.OSHPAZ in user_data.roles and
                UserRole.ADMIN not in user_data.roles and
                UserRole.SUPERADMIN not in user_data.roles
        )

        # Get current filial_id (either from update_data or existing user)
        current_filial_id = update_data.get('filial_id', user['filial_id'])

        if is_oshpaz_only and not current_filial_id:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Oshpaz must be assigned to a filial"
            )

        # Delete existing roles
        delete_roles_query = delete(user_roles).where(user_roles.c.user_id == user_id)
        await database.execute(delete_roles_query)

        # Insert new roles
        for role in user_data.roles:
            role_insert = insert(user_roles).values(user_id=user_id, role=role)
            await database.execute(role_insert)

    # Fetch updated user
    updated_user_query = select(users).where(users.c.id == user_id)
    updated_user = await database.fetch_one(updated_user_query)

    # Get updated roles
    roles_query = select(user_roles.c.role).where(user_roles.c.user_id == user_id)
    roles_result = await database.fetch_all(roles_query)
    user_roles_list = [role['role'] for role in roles_result]

    # Get filial name if assigned
    filial_name = None
    if updated_user['filial_id']:
        filial_query = select(filials.c.name).where(filials.c.id == updated_user['filial_id'])
        filial = await database.fetch_one(filial_query)
        if filial:
            filial_name = filial['name']

    return {
        "id": updated_user['id'],
        "full_name": updated_user['full_name'],
        "phone": updated_user['phone'],
        "username": updated_user['username'],
        "filial_id": updated_user['filial_id'],
        "filial_name": filial_name,
        "is_active": updated_user['is_active'],
        "roles": user_roles_list,
        "created_at": updated_user['created_at']
    }


@router.delete("/{user_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_user(
        user_id: int,
        current_user: dict = Depends(require_admin)
):
    """
    Delete user.
    ADMIN and SUPERADMIN can delete users.
    Cannot delete yourself.
    """
    if user_id == current_user['id']:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="You cannot delete yourself"
        )

    # Check if user exists
    user_query = select(users).where(users.c.id == user_id)
    user = await database.fetch_one(user_query)

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User not found"
        )

    # Delete user (roles will be deleted automatically due to CASCADE)
    delete_query = delete(users).where(users.c.id == user_id)
    await database.execute(delete_query)

    return None