"""add fcm_token to users

Revision ID: a1b2c3d4e5f6
Revises: f91c3d4d6b52
Create Date: 2026-05-05 12:00:00.000000

"""
from typing import Sequence, Union
from alembic import op
import sqlalchemy as sa

revision: str = 'a1b2c3d4e5f6'
down_revision: Union[str, None] = 'f91c3d4d6b52'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column('users', sa.Column('fcm_token', sa.String(512), nullable=True))


def downgrade() -> None:
    op.drop_column('users', 'fcm_token')
