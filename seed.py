import os

from sqlalchemy import insert, select

from database import database, engine
from models.main_models import metadata, users, user_roles, filials, UserRole
from auth import get_password_hash


def create_tables():
    """Create all tables if they don't exist (safe on every startup)."""
    metadata.create_all(engine, checkfirst=True)
    print("✅ Tables ensured")


async def seed_admin():
    """
    Create a superadmin from env vars if SEED_ADMIN_USERNAME and
    SEED_ADMIN_PASSWORD are both set. Does nothing if the user already exists.
    """
    username = os.environ.get("SEED_ADMIN_USERNAME")
    password = os.environ.get("SEED_ADMIN_PASSWORD")
    if not username or not password:
        print("ℹ️ Seed admin: env vars not set, skipping")
        return

    existing = await database.fetch_one(
        select(users).where(users.c.username == username)
    )
    if existing:
        print(f"ℹ️ Seed admin: user '{username}' already exists, skipping")
        return

    full_name = os.environ.get("SEED_ADMIN_FULL_NAME", "Super Admin")
    phone = os.environ.get("SEED_ADMIN_PHONE", "+998000000000")

    user_id = await database.execute(
        insert(users).values(
            full_name=full_name,
            phone=phone,
            username=username,
            password_hash=get_password_hash(password),
            filial_id=None,
            is_active=True,
        )
    )
    await database.execute(
        insert(user_roles).values(user_id=user_id, role=UserRole.SUPERADMIN)
    )
    print(f"✅ Seed admin: superadmin '{username}' created")


async def _get_or_create_seed_filial() -> int:
    """Return id of the seed filial, creating it if missing."""
    name = os.environ.get("SEED_FILIAL_NAME", "Markaziy filial")
    existing = await database.fetch_one(
        select(filials).where(filials.c.name == name)
    )
    if existing:
        return existing["id"]

    filial_id = await database.execute(
        insert(filials).values(
            name=name,
            description="Seed orqali yaratilgan filial",
            is_active=True,
        )
    )
    print(f"✅ Seed filial: '{name}' created")
    return filial_id


async def seed_oshpaz():
    """
    Create an oshpaz from env vars if SEED_OSHPAZ_USERNAME and
    SEED_OSHPAZ_PASSWORD are both set. Oshpaz requires a filial, so the
    seed filial (SEED_FILIAL_NAME) is created first if it doesn't exist.
    Does nothing if the user already exists.
    """
    username = os.environ.get("SEED_OSHPAZ_USERNAME")
    password = os.environ.get("SEED_OSHPAZ_PASSWORD")
    if not username or not password:
        print("ℹ️ Seed oshpaz: env vars not set, skipping")
        return

    existing = await database.fetch_one(
        select(users).where(users.c.username == username)
    )
    if existing:
        print(f"ℹ️ Seed oshpaz: user '{username}' already exists, skipping")
        return

    filial_id = await _get_or_create_seed_filial()

    full_name = os.environ.get("SEED_OSHPAZ_FULL_NAME", "Oshpaz")
    phone = os.environ.get("SEED_OSHPAZ_PHONE", "+998000000001")

    user_id = await database.execute(
        insert(users).values(
            full_name=full_name,
            phone=phone,
            username=username,
            password_hash=get_password_hash(password),
            filial_id=filial_id,
            is_active=True,
        )
    )
    await database.execute(
        insert(user_roles).values(user_id=user_id, role=UserRole.OSHPAZ)
    )
    print(f"✅ Seed oshpaz: '{username}' created (filial_id={filial_id})")
