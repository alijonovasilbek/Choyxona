from datetime import datetime, timedelta
from typing import Optional, List
from jose import JWTError, jwt
from passlib.context import CryptContext
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from config import SECRET_KEY, ALGORITHM, ACCESS_TOKEN_EXPIRE_MINUTES, REFRESH_TOKEN_EXPIRE_DAYS
from database import database
from models.main_models import users, user_roles, UserRole
from sqlalchemy import select, and_

# Password hashing
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/auth/login")


def verify_password(plain_password: str, hashed_password: str) -> bool:
    """Verify a password against its hash."""
    return pwd_context.verify(plain_password, hashed_password)


def get_password_hash(password: str) -> str:
    """Hash a password."""
    return pwd_context.hash(password)


def create_access_token(data: dict, expires_delta: Optional[timedelta] = None) -> str:
    """Create JWT access token."""
    to_encode = data.copy()

    # MUHIM: 'sub' (User ID) doim string bo'lishi shart
    if "sub" in to_encode:
        to_encode["sub"] = str(to_encode["sub"])

    if expires_delta:
        expire = datetime.utcnow() + expires_delta
    else:
        expire = datetime.utcnow() + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)

    to_encode.update({"exp": expire, "type": "access"})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt


def create_refresh_token(data: dict) -> str:
    """Create JWT refresh token."""
    to_encode = data.copy()

    # MUHIM: 'sub' (User ID) doim string bo'lishi shart
    if "sub" in to_encode:
        to_encode["sub"] = str(to_encode["sub"])

    expire = datetime.utcnow() + timedelta(days=REFRESH_TOKEN_EXPIRE_DAYS)
    to_encode.update({"exp": expire, "type": "refresh"})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt


async def get_current_user(token: str = Depends(oauth2_scheme)) -> dict:
    """Get current authenticated user from JWT token."""
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )

    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        user_id_raw = payload.get("sub")
        token_type = payload.get("type")

        if user_id_raw is None or token_type != "access":
            raise credentials_exception

        user_id = int(user_id_raw)  # Stringni bazaga yuborishdan oldin int qilamiz
    except (JWTError, ValueError):
        raise credentials_exception

    # Get user from database
    query = select(users).where(users.c.id == user_id)
    user = await database.fetch_one(query)

    if user is None:
        raise credentials_exception

    if not user['is_active']:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Inactive user"
        )

    # Get user roles
    roles_query = select(user_roles.c.role).where(user_roles.c.user_id == user_id)
    roles_result = await database.fetch_all(roles_query)
    roles = [role['role'] for role in roles_result]

    return {
        "id": user['id'],
        "username": user['username'],
        "full_name": user['full_name'],
        "phone": user['phone'],
        "telegram_chat_id": user['telegram_chat_id'],
        "is_active": user['is_active'],
        "roles": roles
    }


async def get_current_active_user(current_user: dict = Depends(get_current_user)) -> dict:
    """Get current active user."""
    if not current_user['is_active']:
        raise HTTPException(status_code=400, detail="Inactive user")
    return current_user


def require_roles(allowed_roles: List[UserRole]):
    """Dependency to check if user has required roles."""
    async def role_checker(current_user: dict = Depends(get_current_user)):
        user_roles_list = current_user.get('roles', [])

        # Check if user has any of the allowed roles
        if not any(role in allowed_roles for role in user_roles_list):
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="You don't have permission to perform this action"
            )
        return current_user

    return role_checker


# Role dependencies
require_superadmin = require_roles([UserRole.SUPERADMIN])
require_admin = require_roles([UserRole.SUPERADMIN, UserRole.ADMIN])
require_admin_or_oshpaz = require_roles([UserRole.SUPERADMIN, UserRole.ADMIN, UserRole.OSHPAZ])
