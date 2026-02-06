from pydantic import BaseModel, Field, field_validator
from typing import Optional, List
from datetime import date, time, datetime
from enum import Enum


# Enums
class UserRoleEnum(str, Enum):
    SUPERADMIN = "superadmin"
    ADMIN = "admin"
    OSHPAZ = "oshpaz"


class BookingStatusEnum(str, Enum):
    KUTILMOQDA = "kutilmoqda"
    MUVAFFAQIYATLI = "muvaffaqiyatli"
    BEKOR_QILINDI = "bekor_qilindi"


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


class UserResponse(BaseModel):
    id: int
    full_name: str
    phone: str
    username: str
    telegram_chat_id: Optional[str] = None
    is_active: bool
    roles: List[UserRoleEnum]
    created_at: datetime


# ==================== USER MANAGEMENT SCHEMAS ====================
class UserCreate(BaseModel):
    full_name: str = Field(..., min_length=2, max_length=255)
    phone: str = Field(..., pattern=r'^\+?998\d{9}$')
    username: str = Field(..., min_length=3, max_length=100)
    password: str = Field(..., min_length=6)
    telegram_chat_id: Optional[str] = None
    roles: List[UserRoleEnum] = Field(..., min_items=1)


class UserUpdate(BaseModel):
    full_name: Optional[str] = Field(None, min_length=2, max_length=255)
    phone: Optional[str] = Field(None, pattern=r'^\+?998\d{9}$')
    telegram_chat_id: Optional[str] = None
    is_active: Optional[bool] = None
    roles: Optional[List[UserRoleEnum]] = None


# ==================== ROOM SCHEMAS ====================
class RoomCreate(BaseModel):
    name: str = Field(..., min_length=1, max_length=100)
    description: Optional[str] = None
    capacity: int = Field(..., ge=1)


class RoomUpdate(BaseModel):
    name: Optional[str] = Field(None, min_length=1, max_length=100)
    description: Optional[str] = None
    capacity: Optional[int] = Field(None, ge=1)
    is_active: Optional[bool] = None


class RoomResponse(BaseModel):
    id: int
    name: str
    description: Optional[str] = None
    capacity: int
    is_active: bool
    created_at: datetime
    updated_at: datetime


# ==================== BOOKING SCHEMAS ====================
class BookingCreate(BaseModel):
    room_id: int
    booking_date: date
    booking_time: time
    customer_name: str = Field(..., min_length=2, max_length=255)
    customer_phone: str = Field(..., pattern=r'^\+?998\d{9}$')
    guest_count: int = Field(..., ge=1)
    food_description: str = Field(..., min_length=1)
    description: Optional[str] = None


class BookingUpdate(BaseModel):
    room_id: Optional[int] = None
    booking_date: Optional[date] = None
    booking_time: Optional[time] = None
    customer_name: Optional[str] = Field(None, min_length=2, max_length=255)
    customer_phone: Optional[str] = Field(None, pattern=r'^\+?998\d{9}$')
    guest_count: Optional[int] = Field(None, ge=1)
    food_description: Optional[str] = None
    description: Optional[str] = None


class BookingStatusUpdate(BaseModel):
    status: BookingStatusEnum
    total_amount: Optional[float] = Field(None, ge=0)
    cancellation_reason: Optional[str] = None

    @field_validator('total_amount')
    @classmethod
    def validate_amount(cls, v, info):
        if info.data.get('status') == BookingStatusEnum.MUVAFFAQIYATLI and v is None:
            raise ValueError('total_amount is required for successful bookings')
        return v

    @field_validator('cancellation_reason')
    @classmethod
    def validate_cancellation(cls, v, info):
        if info.data.get('status') == BookingStatusEnum.BEKOR_QILINDI and not v:
            raise ValueError('cancellation_reason is required for cancelled bookings')
        return v


class BookingResponse(BaseModel):
    id: int
    room_id: int
    room_name: str
    booking_date: date
    booking_time: time
    customer_name: str
    customer_phone: str
    guest_count: int
    food_description: str
    description: Optional[str] = None
    status: BookingStatusEnum
    total_amount: Optional[float] = None
    cancellation_reason: Optional[str] = None
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
    total_revenue: float
    date_from: Optional[date] = None
    date_to: Optional[date] = None


class MonthlyReportResponse(BaseModel):
    year: int
    month: int
    total_bookings: int
    successful_bookings: int
    cancelled_bookings: int
    pending_bookings: int
    total_revenue: float
    by_room: List[dict]