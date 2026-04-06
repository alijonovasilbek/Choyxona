from pydantic import BaseModel, Field, field_validator
from typing import Optional, List
from datetime import date, datetime, time
from enum import Enum


# Enums
class UserRoleEnum(str, Enum):
    SUPERADMIN = "superadmin"
    ADMIN = "admin"
    OSHPAZ = "oshpaz"


class BookingStatusEnum(str, Enum):
    KUTILMOQDA = "KUTILMOQDA"
    MUVAFFAQIYATLI = "MUVAFFAQIYATLI"
    BEKOR_QILINDI = "BEKOR_QILINDI"
    BOSH= "BOSH"


# ==================== AUTH SCHEMAS ====================
class UserRegister(BaseModel):
    full_name: str = Field(..., min_length=2, max_length=255)
    phone: str = Field(..., pattern=r'^\+?998\d{9}$')
    username: str = Field(..., min_length=3, max_length=100)
    password: str = Field(..., min_length=6)


class UserLogin(BaseModel):
    username: str
    password: str


class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    user_info: dict  # User ma'lumotlari


class UserInfoResponse(BaseModel):
    """Login qilgandan keyin qaytariladigan user ma'lumotlari"""
    id: int
    full_name: str
    username: str
    roles: List[UserRoleEnum]
    user_filial_id: Optional[int] = None  # Oshpaz uchun biriktirilgan filial
    active_filial_id: Optional[int] = None  # Oshpaz uchun o'z filiali, Admin uchun None
    available_filials: List[dict]  # Barcha dostup filiallar
    is_oshpaz: bool  # Oshpazmi yoki yo'qmi


class UserResponse(BaseModel):
    id: int
    full_name: str
    phone: str
    username: str
    filial_id: Optional[int] = None
    filial_name: Optional[str] = None
    is_active: bool
    roles: List[UserRoleEnum]
    created_at: datetime


# ==================== FILIAL SCHEMAS ====================
class FilialCreate(BaseModel):
    name: str = Field(..., min_length=1, max_length=100)
    description: Optional[str] = None


class FilialUpdate(BaseModel):
    name: Optional[str] = Field(None, min_length=1, max_length=100)
    description: Optional[str] = None
    is_active: Optional[bool] = None


class FilialResponse(BaseModel):
    id: int
    name: str
    description: Optional[str] = None
    is_active: bool
    created_at: datetime
    updated_at: datetime


# ==================== USER MANAGEMENT SCHEMAS ====================
class UserCreate(BaseModel):
    full_name: str = Field(..., min_length=2, max_length=255)
    phone: str = Field(..., pattern=r'^\+?998\d{9}$')
    username: str = Field(..., min_length=3, max_length=100)
    password: str = Field(..., min_length=6)
    filial_id: Optional[int] = None  # Oshpaz uchun filial
    roles: List[UserRoleEnum] = Field(..., min_items=1)


class UserUpdate(BaseModel):
    full_name: Optional[str] = Field(None, min_length=2, max_length=255)
    phone: Optional[str] = Field(None, pattern=r'^\+?998\d{9}$')
    filial_id: Optional[int] = None  # Oshpaz uchun filial
    is_active: Optional[bool] = None
    roles: Optional[List[UserRoleEnum]] = None


# ==================== ROOM SCHEMAS ====================
class RoomCreate(BaseModel):
    name: str = Field(..., min_length=1, max_length=100)
    filial_id: int  # Qaysi filial (Admin/Superadmin qo'shganda majburiy)
    description: Optional[str] = None


class RoomUpdate(BaseModel):
    name: Optional[str] = Field(None, min_length=1, max_length=100)
    filial_id: Optional[int] = None
    description: Optional[str] = None
    is_active: Optional[bool] = None


class RoomResponse(BaseModel):
    id: int
    name: str
    filial_id: int
    filial_name: Optional[str] = None
    description: Optional[str] = None
    is_active: bool
    created_at: datetime
    updated_at: datetime


# ==================== BOOKING SCHEMAS ====================
class BookingCreate(BaseModel):
    room_id: int
    booking_date: date
    description: Optional[str] = None


class BookingUpdate(BaseModel):
    room_id: Optional[int] = None
    booking_date: Optional[date] = None
    description: Optional[str] = None


class BookingStatusUpdate(BaseModel):
    status: BookingStatusEnum


class BookingResponse(BaseModel):
    id: int
    room_id: int
    room_name: str
    filial_id: int
    filial_name: Optional[str] = None
    booking_date: date
    description: Optional[str] = None
    status: BookingStatusEnum
    created_by: int
    created_by_name: str
    created_at: datetime
    updated_at: datetime


class SaboyCreate(BaseModel):
    filial_id: int
    saboy_date: date
    saboy_time: time
    description: str = Field(..., min_length=1)

    @field_validator("description")
    @classmethod
    def validate_description(cls, value: str) -> str:
        value = value.strip()
        if not value:
            raise ValueError("Description cannot be empty")
        return value


class SaboyUpdate(BaseModel):
    filial_id: Optional[int] = None
    saboy_date: Optional[date] = None
    saboy_time: Optional[time] = None
    description: Optional[str] = Field(None, min_length=1)

    @field_validator("description")
    @classmethod
    def validate_description(cls, value: Optional[str]) -> Optional[str]:
        if value is None:
            return value
        value = value.strip()
        if not value:
            raise ValueError("Description cannot be empty")
        return value


class SaboyResponse(BaseModel):
    id: int
    filial_id: int
    filial_name: Optional[str] = None
    saboy_date: date
    saboy_time: time
    description: str
    created_by: int
    created_by_name: str
    created_at: datetime
    updated_at: datetime


# ==================== REPORT SCHEMAS ====================
class BookingStatsResponse(BaseModel):
    total_bookings: int
    kutilmoqda_count: int
    muvaffaqiyatli_count: int
    bekor_qilindi_count: int
    date_from: Optional[date] = None
    date_to: Optional[date] = None


class MonthlyReportResponse(BaseModel):
    year: int
    month: int
    total_bookings: int
    successful_bookings: int
    cancelled_bookings: int
    pending_bookings: int
    by_room: List[dict]
