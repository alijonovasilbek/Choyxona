import logging
import os
import firebase_admin
from firebase_admin import credentials, messaging

logger = logging.getLogger(__name__)

SERVICE_ACCOUNT_PATH = os.path.join(os.path.dirname(__file__), '..', 'firebase_service_account.json')

_initialized = False


def _init_firebase():
    global _initialized
    if not _initialized:
        cred = credentials.Certificate(SERVICE_ACCOUNT_PATH)
        firebase_admin.initialize_app(cred)
        _initialized = True


async def send_fcm_notification(fcm_token: str, title: str, body: str) -> bool:
    try:
        _init_firebase()
        message = messaging.Message(
            notification=messaging.Notification(title=title, body=body),
            android=messaging.AndroidConfig(priority='high'),
            token=fcm_token,
        )
        messaging.send(message)
        return True
    except Exception as e:
        logger.error(f"FCM send error for token {fcm_token[:20]}...: {e}")
        return False


async def send_fcm_to_many(fcm_tokens: list[str], title: str, body: str) -> int:
    """Returns number of successfully sent notifications."""
    if not fcm_tokens:
        return 0
    try:
        _init_firebase()
        message = messaging.MulticastMessage(
            notification=messaging.Notification(title=title, body=body),
            android=messaging.AndroidConfig(priority='high'),
            tokens=fcm_tokens,
        )
        response = messaging.send_each_for_multicast(message)
        logger.info(f"FCM multicast: {response.success_count} sent, {response.failure_count} failed")
        return response.success_count
    except Exception as e:
        logger.error(f"FCM multicast error: {e}")
        return 0
