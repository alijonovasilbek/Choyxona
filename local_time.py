"""Business timezone helpers.

The server and the postgres container both run in UTC, but the choyxona works
on Tashkent time (UTC+5). Using `date.today()` directly meant that between
00:00 and 05:00 local the backend still believed it was the previous day, so
"today" was rejected as a past date while "yesterday" was accepted.
"""

import os
from datetime import date, datetime
from zoneinfo import ZoneInfo

APP_TIMEZONE = ZoneInfo(os.environ.get("APP_TIMEZONE", "Asia/Tashkent"))


def now_local() -> datetime:
    """Current time in the business timezone."""
    return datetime.now(APP_TIMEZONE)


def today_local() -> date:
    """Current date in the business timezone."""
    return now_local().date()
