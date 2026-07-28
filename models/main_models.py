from sqlalchemy import MetaData, Table, Column, Integer, String, Float, DateTime, Boolean, ForeignKey, Enum, Text, Date, Time
from datetime import datetime
import enum
from sqlalchemy import func

metadata = MetaData()


# Enums
class UserRole(str, enum.Enum):
    SUPERADMIN = "superadmin"
    ADMIN = "admin"
    OSHPAZ = "oshpaz"


class BookingStatus(str, enum.Enum):
    KUTILMOQDA = "KUTILMOQDA"  # Pending
    MUVAFFAQIYATLI = "MUVAFFAQIYATLI"  # Successful
    BEKOR_QILINDI = "BEKOR_QILINDI"  # Cancelled
    BOSH= "BOSH"


# Filials (Branches) table
filials = Table(
    'filials',
    metadata,
    Column('id', Integer, primary_key=True, autoincrement=True),
    Column('name', String(100), nullable=False, unique=True),  # e.g., "1-FILIAL", "2-FILIAL"
    Column('description', Text, nullable=True),
    Column('is_active', Boolean, default=True),
    # Saboy faqat ba'zi filiallarda bo'ladi (masalan Markaziyda yo'q).
    Column('has_saboy', Boolean, nullable=False, server_default='false'),
    Column('created_at', DateTime, nullable=False, server_default=func.now()),
    Column('updated_at', DateTime, nullable=False, server_default=func.now(), onupdate=func.now()),
)


# Users table
users = Table(
    'users',
    metadata,
    Column('id', Integer, primary_key=True, autoincrement=True),
    Column('full_name', String(255), nullable=False),
    Column('phone', String(20), unique=True, nullable=False),
    Column('username', String(100), unique=True, nullable=False),
    Column('password_hash', String(255), nullable=False),
    Column('filial_id', Integer, ForeignKey('filials.id', ondelete='SET NULL'), nullable=True),  # Oshpaz uchun
    Column('fcm_token', String(512), nullable=True),
    Column('is_active', Boolean, default=True),
    Column('created_at', DateTime(timezone=True), nullable=False, server_default=func.now()),
    Column('updated_at', DateTime(timezone=True), nullable=False, server_default=func.now(), onupdate=func.now()),
)


# User Roles (Many-to-many relation)
user_roles = Table(
    'user_roles',
    metadata,
    Column('id', Integer, primary_key=True, autoincrement=True),
    Column('user_id', Integer, ForeignKey('users.id', ondelete='CASCADE'), nullable=False),
    Column('role', Enum(UserRole), nullable=False),
    Column('assigned_at', DateTime, nullable=False, server_default=func.now()),
)


# Rooms/Places table
rooms = Table(
    'rooms',
    metadata,
    Column('id', Integer, primary_key=True, autoincrement=True),
    Column('name', String(100), nullable=False),  # e.g., "1-xona", "2-xona", "1-so'ri"
    Column('filial_id', Integer, ForeignKey('filials.id', ondelete='CASCADE'), nullable=False),  # Qaysi filial
    Column('description', Text, nullable=True),
    Column('is_active', Boolean, default=True),
    Column('created_at', DateTime, nullable=False, server_default=func.now()),
    Column('updated_at', DateTime, nullable=False, server_default=func.now(), onupdate=func.now()),
)


# Bookings table
bookings = Table(
    'bookings',
    metadata,
    Column('id', Integer, primary_key=True, autoincrement=True),
    Column('room_id', Integer, ForeignKey('rooms.id', ondelete='CASCADE'), nullable=False),
    Column('booking_date', Date, nullable=False),  # Qaysi sana
    Column('description', Text, nullable=True),  # Qo'shimcha ma'lumot
    Column('status', Enum(BookingStatus), default=BookingStatus.KUTILMOQDA),
    Column('created_by', Integer, ForeignKey('users.id'), nullable=False),  # Kim yaratdi
    Column('created_at', DateTime, nullable=False, server_default=func.now()),
    Column('updated_at', DateTime, nullable=False, server_default=func.now(), onupdate=func.now()),
)


# Saboys table
saboys = Table(
    'saboys',
    metadata,
    Column('id', Integer, primary_key=True, autoincrement=True),
    Column('filial_id', Integer, ForeignKey('filials.id', ondelete='CASCADE'), nullable=False),
    Column('saboy_date', Date, nullable=False),
    Column('saboy_time', Time, nullable=False),
    Column('description', Text, nullable=False),
    Column('created_by', Integer, ForeignKey('users.id'), nullable=False),
    Column('created_at', DateTime, nullable=False, server_default=func.now()),
    Column('updated_at', DateTime, nullable=False, server_default=func.now(), onupdate=func.now()),
)


# Notification logs (optional, for tracking sent notifications)
notifications = Table(
    'notifications',
    metadata,
    Column('id', Integer, primary_key=True, autoincrement=True),
    Column('booking_id', Integer, ForeignKey('bookings.id', ondelete='CASCADE'), nullable=False),
    Column('user_id', Integer, ForeignKey('users.id', ondelete='CASCADE'), nullable=False),
    Column('message', Text, nullable=False),
    Column('sent_at', DateTime, nullable=False, server_default=func.now()),
    Column('is_sent', Boolean, default=False),
)
