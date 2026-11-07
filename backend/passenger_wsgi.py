#
# import os
# import sys
# import logging
#
# BASE_DIR = os.path.dirname(os.path.abspath(__file__))
#
# # Make the backend package importable
# if BASE_DIR not in sys.path:
#     sys.path.insert(0, BASE_DIR)
#
# # Log errors to a file
# logging.basicConfig(
#     filename=os.path.join(BASE_DIR, "passenger_error.log"),
#     level=logging.INFO,
#     format="%(asctime)s %(levelname)s %(message)s",
# )
#
# try:
#     from a2wsgi import ASGIMiddleware
#     from app.main import app as fastapi_app
#
#     # WSGI entry point required by Passenger
#     application = ASGIMiddleware(fastapi_app)
#
#     logging.info("FastAPI application loaded successfully")
#
# except Exception:
#     logging.exception("Failed to load FastAPI application")
#     raise



import sys
import os

base_dir = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, base_dir)
sys.path.insert(0, os.path.join(base_dir, "api"))

log_file = os.path.join(base_dir, "passenger_error.log")


def log_msg(msg):
    try:
        with open(log_file, "a", encoding="utf-8") as f:
            import datetime
            f.write(f"[{datetime.datetime.now()}] {msg}\n")
    except Exception:
        pass


log_msg("=== PASSENGER WSGI STARTUP ===")

try:
    from app.main import app

    log_msg("[OK] FastAPI app imported")

    HTTP_STATUS_PHRASES = {
        200: "OK",
        201: "Created",
        204: "No Content",
        301: "Moved Permanently",
        302: "Found",
        304: "Not Modified",
        307: "Temporary Redirect",
        308: "Permanent Redirect",
        400: "Bad Request",
        401: "Unauthorized",
        403: "Forbidden",
        404: "Not Found",
        405: "Method Not Allowed",
        409: "Conflict",
        422: "Unprocessable Entity",
        429: "Too Many Requests",
        500: "Internal Server Error",
        502: "Bad Gateway",
        503: "Service Unavailable",
    }

    def application(environ, start_response):
        """WSGI wrapper for the FastAPI ASGI application."""
        import asyncio
        import traceback

        method = environ["REQUEST_METHOD"]
        path = environ.get("PATH_INFO", "/")

        log_msg(f"[REQUEST] {method} {path}")

        try:
            try:
                content_length = int(
                    environ.get("CONTENT_LENGTH") or 0
                )
            except (ValueError, TypeError):
                content_length = 0

            if content_length > 0:
                request_body = environ["wsgi.input"].read(
                    content_length
                )
            else:
                request_body = b""

            headers = []

            content_type = environ.get("CONTENT_TYPE")
            if content_type:
                headers.append(
                    (
                        b"content-type",
                        content_type.encode("latin-1"),
                    )
                )

            if content_length:
                headers.append(
                    (
                        b"content-length",
                        str(content_length).encode("latin-1"),
                    )
                )

            for key, value in environ.items():
                if key.startswith("HTTP_"):
                    header_name = (
                        key[5:]
                        .replace("_", "-")
                        .lower()
                        .encode("latin-1")
                    )
                    headers.append(
                        (header_name, value.encode("latin-1"))
                    )

            scope = {
                "type": "http",
                "asgi": {"version": "3.0"},
                "http_version": "1.1",
                "method": method,
                "scheme": environ.get(
                    "wsgi.url_scheme", "http"
                ),
                "path": path,
                "query_string": environ.get(
                    "QUERY_STRING", ""
                ).encode(),
                "root_path": environ.get("SCRIPT_NAME", ""),
                "headers": headers,
                "server": (
                    environ.get("SERVER_NAME", "localhost"),
                    int(environ.get("SERVER_PORT", 80)),
                ),
                "client": (
                    environ.get("REMOTE_ADDR", "127.0.0.1"),
                    int(environ.get("REMOTE_PORT", 0) or 0),
                ),
            }

            resp_status = 200
            resp_headers = []
            body_parts = []

            async def receive():
                return {
                    "type": "http.request",
                    "body": request_body,
                    "more_body": False,
                }

            async def send(message):
                nonlocal resp_status, resp_headers, body_parts

                if message["type"] == "http.response.start":
                    resp_status = message["status"]
                    resp_headers = message.get("headers", [])

                elif message["type"] == "http.response.body":
                    body = message.get("body", b"")
                    if body:
                        body_parts.append(body)

            async def run_app():
                await app(scope, receive, send)

            asyncio.run(run_app())

            wsgi_headers = [
                (
                    name.decode("latin-1")
                    if isinstance(name, bytes)
                    else name,
                    value.decode("latin-1")
                    if isinstance(value, bytes)
                    else value,
                )
                for name, value in resp_headers
            ]

            reason = HTTP_STATUS_PHRASES.get(
                resp_status, "Unknown"
            )

            start_response(
                f"{resp_status} {reason}",
                wsgi_headers,
            )

            log_msg(
                f"[RESPONSE] {method} {path} "
                f"- Status: {resp_status}"
            )

            return body_parts if body_parts else [b""]

        except Exception as e:
            log_msg(
                f"[ERROR] {type(e).__name__}: {str(e)}"
            )
            log_msg(traceback.format_exc())

            start_response(
                "500 Internal Server Error",
                [("Content-Type", "text/plain")],
            )

            return [b"Internal Server Error"]

    log_msg("[OK] WSGI application ready")

except Exception as e:
    log_msg(
        f"[CRITICAL] {type(e).__name__}: {str(e)}"
    )

    import traceback
    log_msg(traceback.format_exc())
