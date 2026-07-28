from fastapi import APIRouter, HTTPException, status, Depends
from sqlalchemy import select, insert, func
from database import database
from models.main_models import users, user_roles, filials, UserRole
from schemas import UserRegister, UserLogin, TokenResponse, UserResponse, UserInfoResponse
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
        "is_active": created_user['is_active'],
        "roles": [UserRole.SUPERADMIN],
        "created_at": created_user['created_at']
    }


@router.post("/login", response_model=TokenResponse)
async def login(user_credentials: UserLogin):
    """
    Login endpoint (JSON).

    Use this endpoint for API calls with JSON body.

    Process:
    1. Admin/Superadmin: Can access all filials (no filial_id in token)
    2. Oshpaz: Can only access their assigned filial
    """
    return await _perform_login(user_credentials.username, user_credentials.password)


@router.post("/token", response_model=TokenResponse)
async def login_for_swagger(form_data: OAuth2PasswordRequestForm = Depends()):
    """
    OAuth2 compatible token login (for Swagger UI).

    This endpoint is specifically for Swagger's "Authorize" button.
    For API calls, use /auth/login instead.
    """
    return await _perform_login(form_data.username, form_data.password)


async def _perform_login(username: str, password: str):
    """
    Common login logic for both endpoints.
    """
    # Find user by username
    user_query = select(users).where(users.c.username == username)
    user = await database.fetch_one(user_query)

    if not user or not verify_password(password, user['password_hash']):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password"
        )

    # Get user roles
    roles_query = select(user_roles.c.role).where(user_roles.c.user_id == user['id'])
    roles_result = await database.fetch_all(roles_query)
    user_roles_list = [role['role'] for role in roles_result]

    # Check if user is Oshpaz only
    is_oshpaz_only = (
            UserRole.OSHPAZ in user_roles_list and
            UserRole.ADMIN not in user_roles_list and
            UserRole.SUPERADMIN not in user_roles_list
    )

    # Determine access level
    if is_oshpaz_only:
        # Oshpaz: use assigned filial only
        if not user['filial_id']:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Oshpaz must be assigned to a filial"
            )
        active_filial_id = user['filial_id']

        # Oshpaz starts in their assigned filial but may switch to any active one,
        # so the full list is returned for the header switcher.
        filials_query = select(filials).where(filials.c.is_active == True).order_by(filials.c.name)
        all_filials = await database.fetch_all(filials_query)
        available_filials = [
            {"id": f['id'], "name": f['name']}
            for f in all_filials
        ]

    else:
        # Admin/Superadmin: can access all filials
        active_filial_id = None  # No specific filial in token

        # Get all available filials
        filials_query = select(filials).where(filials.c.is_active == True).order_by(filials.c.name)
        all_filials = await database.fetch_all(filials_query)
        available_filials = [
            {"id": f['id'], "name": f['name']}
            for f in all_filials
        ]

    # Create tokens
    token_data = {
        "sub": user['id'],
        "filial_id": active_filial_id  # None for Admin/Superadmin, specific for Oshpaz
    }

    access_token = create_access_token(data=token_data)
    refresh_token = create_refresh_token(data=token_data)

    return {
        "access_token": access_token,
        "refresh_token": refresh_token,
        "token_type": "bearer",
        "user_info": {
            "id": user['id'],
            "full_name": user['full_name'],
            "username": user['username'],
            "roles": user_roles_list,
            "user_filial_id": user['filial_id'],
            "active_filial_id": active_filial_id,
            "available_filials": available_filials,
            "is_oshpaz": is_oshpaz_only
        }
    }


@router.get("/available-filials")
async def get_available_filials():
    """
    Get list of available filials for login selection.
    Used by frontend before login for Admin/Superadmin.
    """
    filials_query = select(filials).where(filials.c.is_active == True).order_by(filials.c.name)
    available_filials = await database.fetch_all(filials_query)

    return {
        "filials": [
            {
                "id": f['id'],
                "name": f['name'],
                "description": f['description']
            }
            for f in available_filials
        ]
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

    # Get active filial name if available
    filial_name = None
    if current_user.get('active_filial_id'):
        filial_query = select(filials.c.name).where(filials.c.id == current_user['active_filial_id'])
        filial = await database.fetch_one(filial_query)
        if filial:
            filial_name = filial['name']

    return {
        "id": current_user['id'],
        "full_name": current_user['full_name'],
        "phone": user_data['phone'],
        "username": current_user['username'],
        "filial_id": current_user.get('active_filial_id'),
        "filial_name": filial_name,
        "is_active": current_user['is_active'],
        "roles": current_user['roles'],
        "created_at": user_data['created_at']
    }


@router.post("/switch-filial", response_model=TokenResponse)
async def switch_filial(
        filial_id: int,
        current_user: dict = Depends(get_current_user)
):
    """
    Switch active filial. Available to every role — an oshpaz keeps their
    assigned filial as the default but may work in any active filial.
    """
    # Verify filial exists and is active
    filial_query = select(filials).where(filials.c.id == filial_id)
    filial = await database.fetch_one(filial_query)

    if not filial:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Filial not found"
        )

    if not filial['is_active']:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Filial is not active"
        )

    # Create new tokens with new filial_id
    token_data = {
        "sub": current_user['id'],
        "filial_id": filial_id
    }

    access_token = create_access_token(data=token_data)
    refresh_token = create_refresh_token(data=token_data)

    # The client keeps a header switcher populated from this list, so it must be
    # returned on every switch — not only at login.
    filials_query = select(filials).where(filials.c.is_active == True).order_by(filials.c.name)
    all_filials = await database.fetch_all(filials_query)
    available_filials = [{"id": f['id'], "name": f['name']} for f in all_filials]

    return {
        "access_token": access_token,
        "refresh_token": refresh_token,
        "token_type": "bearer",
        "user_info": {
            "id": current_user['id'],
            "full_name": current_user['full_name'],
            "username": current_user['username'],
            "roles": current_user['roles'],
            "user_filial_id": current_user.get('user_filial_id'),
            "active_filial_id": filial_id,
            "active_filial_name": filial['name'],
            "available_filials": available_filials,
            "is_oshpaz": (
                UserRole.OSHPAZ in current_user['roles'] and
                UserRole.ADMIN not in current_user['roles'] and
                UserRole.SUPERADMIN not in current_user['roles']
            )
        }
    }