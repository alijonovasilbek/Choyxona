from sqlalchemy import MetaData, Table, Column, Integer, String, Float, DateTime, Boolean, ForeignKey, Enum, Text, Date, Time
from datetime import datetime
import enum
from sqlalchemy import DateTime, func

metadata = MetaData()


# Enums
class UserRole(str, enum.Enum):
    SUPERADMIN = "superadmin"
    ADMIN = "admin"
    OSHPAZ = "oshpaz"


class BookingStatus(str, enum.Enum):
    KUTILMOQDA = "kutilmoqda"  # Pending
    MUVAFFAQIYATLI = "muvaffaqiyatli"  # Successful
    BEKOR_QILINDI = "bekor_qilindi"  # Cancelled


# Users table
users = Table(
    'users',
    metadata,
    Column('id', Integer, primary_key=True, autoincrement=True),
    Column('full_name', String(255), nullable=False),
    Column('phone', String(20), unique=True, nullable=False),
    Column('username', String(100), unique=True, nullable=False),
    Column('password_hash', String(255), nullable=False),
    Column('telegram_chat_id', String(100), nullable=True),  # For notifications
    Column('is_active', Boolean, default=True),
    Column(
        'created_at',
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False
    ),
    Column(
        'updated_at',
        DateTime(timezone=True),
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False
    ),
)


# User Roles (Many-to-many relation)
user_roles = Table(
    'user_roles',
    metadata,
    Column('id', Integer, primary_key=True, autoincrement=True),
    Column('user_id', Integer, ForeignKey('users.id', ondelete='CASCADE'), nullable=False),
    Column('role', Enum(UserRole), nullable=False),
    Column('assigned_at', DateTime, default=datetime.utcnow),
)


# Rooms/Places table
rooms = Table(
    'rooms',
    metadata,
    Column('id', Integer, primary_key=True, autoincrement=True),
    Column('name', String(100), nullable=False),  # e.g., "1-xona", "2-xona", "1-so'ri"
    Column('description', Text, nullable=True),
    Column('capacity', Integer, nullable=False),  # Nechta odam sig'adi
    Column('is_active', Boolean, default=True),
    Column('created_at', DateTime, default=datetime.utcnow),
    Column('updated_at', DateTime, default=datetime.utcnow, onupdate=datetime.utcnow),
)


# Bookings table
bookings = Table(
    'bookings',
    metadata,
    Column('id', Integer, primary_key=True, autoincrement=True),
    Column('room_id', Integer, ForeignKey('rooms.id', ondelete='CASCADE'), nullable=False),
    Column('booking_date', Date, nullable=False),  # Qaysi sana
    Column('booking_time', Time, nullable=False),  # Soat nechada
    Column('customer_name', String(255), nullable=False),  # Buyurtmachi ismi
    Column('customer_phone', String(20), nullable=False),  # Telefon raqami
    Column('guest_count', Integer, nullable=False),  # Nechi kishi
    Column('food_description', Text, nullable=False),  # Qanday ovqat (masalan: 2kg osh)
    Column('description', Text, nullable=True),  # Qo'shimcha ma'lumot
    Column('status', Enum(BookingStatus), default=BookingStatus.KUTILMOQDA),
    Column('total_amount', Float, nullable=True),  # Summa (faqat muvaffaqiyatli bo'lganda)
    Column('cancellation_reason', Text, nullable=True),  # Bekor qilish sababi
    Column('created_by', Integer, ForeignKey('users.id'), nullable=False),  # Kim yaratdi
    Column('created_at', DateTime, default=datetime.utcnow),
    Column('updated_at', DateTime, default=datetime.utcnow, onupdate=datetime.utcnow),
)


# Notification logs (optional, for tracking sent notifications)
notifications = Table(
    'notifications',
    metadata,
    Column('id', Integer, primary_key=True, autoincrement=True),
    Column('booking_id', Integer, ForeignKey('bookings.id', ondelete='CASCADE'), nullable=False),
    Column('user_id', Integer, ForeignKey('users.id', ondelete='CASCADE'), nullable=False),
    Column('message', Text, nullable=False),
    Column('sent_at', DateTime, default=datetime.utcnow),
    Column('is_sent', Boolean, default=False),
)
