from fastapi import APIRouter, HTTPException, status, Depends
from sqlalchemy import select, insert, func
from database import database
from models.main_models import users, user_roles, UserRole
from schemas import UserRegister, UserLogin, TokenResponse, UserResponse
from fastapi.security import OAuth2PasswordRequestForm
from auth import (
    get_password_hash,
    verify_password,
    create_access_token,
    create_refresh_token,
    get_current_user
)
from typing import List

router = APIRouter(prefix="/auth", tags=["Authentication"])


@router.post("/register", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
async def register(user_data: UserRegister):
    """
    Register first user as SUPERADMIN.
    Only works if no users exist in the database.
    """
    # Check if any user exists
    count_query = select(func.count()).select_from(users)
    user_count = await database.fetch_val(count_query)

    if user_count > 0:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Registration is closed. Superadmin already exists."
        )

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

    # Hash password
    hashed_password = get_password_hash(user_data.password)

    # Insert user
    insert_query = insert(users).values(
        full_name=user_data.full_name,
        phone=user_data.phone,
        username=user_data.username,
        password_hash=hashed_password,
        is_active=True
    )
    user_id = await database.execute(insert_query)

    # Assign SUPERADMIN role
    role_insert = insert(user_roles).values(
        user_id=user_id,
        role=UserRole.SUPERADMIN
    )
    await database.execute(role_insert)

    # Fetch created user
    user_query = select(users).where(users.c.id == user_id)
    created_user = await database.fetch_one(user_query)

    return {
        "id": created_user['id'],
        "full_name": created_user['full_name'],
        "phone": created_user['phone'],
        "username": created_user['username'],
        "telegram_chat_id": created_user['telegram_chat_id'],
        "is_active": created_user['is_active'],
        "roles": [UserRole.SUPERADMIN],
        "created_at": created_user['created_at']
    }


@router.post("/login", response_model=TokenResponse)
async def login(user_credentials: OAuth2PasswordRequestForm = Depends()):
    """
    Login endi ham JSON, ham Form ma'lumotlarini qabul qila oladi.
    """
    # OAuth2PasswordRequestForm'da 'username' va 'password' maydonlari bo'ladi
    user_query = select(users).where(users.c.username == user_credentials.username)
    user = await database.fetch_one(user_query)

    if not user or not verify_password(user_credentials.password, user['password_hash']):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password"
        )

    # ... qolgan kod o'zgarishsiz qoladi ...
    access_token = create_access_token(data={"sub": user['id']})
    refresh_token = create_refresh_token(data={"sub": user['id']})

    return {
        "access_token": access_token,
        "refresh_token": refresh_token,
        "token_type": "bearer"
    }


@router.get("/me", response_model=UserResponse)
async def get_me(current_user: dict = Depends(get_current_user)):
    """Get current authenticated user information."""
    # Fetch full user data from database to get created_at
    user_query = select(users).where(users.c.id == current_user['id'])
    user_data = await database.fetch_one(user_query)

    if not user_data:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User not found"
        )

    return {
        "id": current_user['id'],
        "full_name": current_user['full_name'],
        "phone": current_user['phone'],
        "username": current_user['username'],
        "telegram_chat_id": current_user['telegram_chat_id'],
        "is_active": current_user['is_active'],
        "roles": current_user['roles'],
        "created_at": user_data['created_at']
    }