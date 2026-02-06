from databases import Database
from sqlalchemy import create_engine
from config import DB_NAME, DB_USER, DB_PASSWORD, DB_HOST, DB_PORT

# Database URL
DATABASE_URL = f"postgresql+asyncpg://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"

# Create async database instance
database = Database(DATABASE_URL)

# For Alembic migrations (synchronous)
SYNC_DATABASE_URL = f"postgresql+psycopg2://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"
engine = create_engine(SYNC_DATABASE_URL)