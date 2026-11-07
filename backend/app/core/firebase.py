"""Firebase Admin SDK helper — initialised once at startup."""
import logging
from typing import Any

logger = logging.getLogger(__name__)

_app = None


def init_firebase(credentials_path: str) -> None:
    """Call once at app startup. Safe to call even if credentials_path is empty (FCM is disabled)."""
    global _app
    if not credentials_path:
        logger.info("FCM disabled: SD_FIREBASE_CREDENTIALS_PATH not set")
        return
    try:
        import firebase_admin
        from firebase_admin import credentials
        if not firebase_admin._apps:
            cred = credentials.Certificate(credentials_path)
            _app = firebase_admin.initialize_app(cred)
            logger.info("Firebase Admin SDK initialised")
    except Exception as exc:
        logger.warning("Firebase Admin SDK init failed: %s", exc)


def send_to_tokens(tokens: list[str], title: str, body: str, data: dict[str, str] | None = None) -> dict[str, Any]:
    """
    Send a push notification to a list of FCM tokens (max 500 per call — batched automatically).
    Returns {"success": n, "failure": n, "responses": [...]}.
    Does nothing and returns zeros if Firebase is not initialised.
    """
    if not tokens:
        return {"success": 0, "failure": 0, "responses": []}
    try:
        from firebase_admin import messaging
    except ImportError:
        logger.warning("firebase-admin not installed — pip install firebase-admin")
        return {"success": 0, "failure": 0, "responses": []}

    if _app is None:
        logger.warning("Firebase not initialised — skipping FCM send")
        return {"success": 0, "failure": 0, "responses": []}

    success_total = failure_total = 0
    all_responses: list[dict] = []

    # FCM multicast accepts max 500 tokens per call
    for i in range(0, len(tokens), 500):
        batch = tokens[i : i + 500]
        message = messaging.MulticastMessage(
            tokens=batch,
            notification=messaging.Notification(title=title, body=body),
            data={k: str(v) for k, v in (data or {}).items()},
            android=messaging.AndroidConfig(priority="high"),
        )
        result = messaging.send_each_for_multicast(message)
        success_total += result.success_count
        failure_total += result.failure_count
        for resp in result.responses:
            all_responses.append({
                "success": resp.success,
                "message_id": resp.message_id if resp.success else None,
                "error": str(resp.exception) if not resp.success else None,
            })

    return {"success": success_total, "failure": failure_total, "responses": all_responses}


def send_to_topic(topic: str, title: str, body: str, data: dict[str, str] | None = None) -> str | None:
    """
    Send to a Firebase topic (e.g. 'all_users').
    Returns the message ID or None if Firebase is not initialised.
    """
    try:
        from firebase_admin import messaging
    except ImportError:
        return None
    if _app is None:
        return None
    message = messaging.Message(
        topic=topic,
        notification=messaging.Notification(title=title, body=body),
        data={k: str(v) for k, v in (data or {}).items()},
        android=messaging.AndroidConfig(priority="high"),
    )
    return messaging.send(message)
