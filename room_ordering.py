"""Shared ordering rules for room/place names.

Room names look like "1-XONA", "10-SO'RI", "2 xona". Business rule: inside a
filial every XONA comes before every SO'RI, and inside a type the numbers are
ordered numerically (1, 2, ... 10, 11) instead of lexicographically.

The apostrophe in SO'RI is typed with several different characters depending on
the keyboard, so names are normalized before the type is detected.
"""

import re

# Straight quote, curly quotes, Uzbek okina/tortoq, backtick, acute accent.
APOSTROPHES = "'‘’ʻʼ`´"
_APOSTROPHE_TRANSLATION = {ord(char): None for char in APOSTROPHES}

_NUMBER_PATTERN = re.compile(r"\d+")

# Lower value sorts first.
ROOM_TYPE_PRIORITY = (
    ("XONA", 0),
    ("SORI", 1),
)
UNKNOWN_TYPE_PRIORITY = 2

# Rooms without a number go after the numbered ones of the same type.
_UNNUMBERED = 10 ** 9


def normalize_room_name(name) -> str:
    """Uppercase and strip apostrophe variants so SO'RI/SOʻRI/SORI all match."""
    return (name or "").upper().translate(_APOSTROPHE_TRANSLATION).strip()


def room_type_priority(normalized_name: str) -> int:
    for token, priority in ROOM_TYPE_PRIORITY:
        if token in normalized_name:
            return priority
    return UNKNOWN_TYPE_PRIORITY


def room_order_key(name, filial_name=None):
    """Sort key: filial, then XONA before SO'RI, then numeric, then name."""
    normalized = normalize_room_name(name)
    match = _NUMBER_PATTERN.search(normalized)
    number = int(match.group()) if match else _UNNUMBERED

    return (
        filial_name or "",
        room_type_priority(normalized),
        number,
        normalized
    )
