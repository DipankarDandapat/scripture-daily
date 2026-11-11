"""Phone-sized HTML preview of the Android maintenance screen (GET /api/v1/admin/maintenance/preview).

Mirrors android/.../gate/MaintenanceScreen.kt closely enough for an administrator to check the texts and the estimated
completion time before (or while) maintenance is switched on.
"""
from datetime import datetime
from html import escape
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

_THEMES = {
    "light": {"bg": "#FBF6EC", "surface": "#FFFFFF", "ink": "#2A2140", "mid": "#6B6380", "gold": "#C8963E", "purple": "#4B2E83", "on": "#FFFFFF"},
    "dark": {"bg": "#14101F", "surface": "#1F1830", "ink": "#F3ECDD", "mid": "#B7AECB", "gold": "#E2B866", "purple": "#B9A2F0", "on": "#14101F"},
}


def _local_time(iso: str | None, tz_name: str) -> str | None:
    if not iso:
        return None
    try:
        moment = datetime.fromisoformat(iso.replace("Z", "+00:00"))
        tz = ZoneInfo(tz_name)
    except (ValueError, ZoneInfoNotFoundError):
        return iso
    return moment.astimezone(tz).strftime("%a %d %b, %H:%M ") + tz_name


def render_maintenance_preview(cfg: dict, theme: str = "light", tz_name: str = "UTC") -> str:
    c = _THEMES.get(theme, _THEMES["light"])
    title = escape(cfg.get("title") or "We'll Be Back Soon!")
    message = escape(cfg.get("message") or "")
    eta = _local_time(cfg.get("estimated_end_time"), tz_name)
    eta_html = f'<div class="eta">We\'ll be back around <b>{escape(eta)}</b></div>' if eta else ""
    retry = '<button class="secondary">Check Again</button>' if cfg.get("allow_retry", True) else ""
    state = "ENABLED — users are blocked" if cfg.get("enabled") else "disabled — users are not affected"
    return f"""<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Maintenance preview</title>
<style>
  body {{ margin:0; min-height:100vh; display:flex; flex-direction:column; align-items:center; justify-content:center; gap:16px;
         background:#E9E4DA; font-family: -apple-system, 'Segoe UI', Roboto, sans-serif; }}
  .state {{ font-size:13px; color:#333; }}
  .phone {{ width:360px; height:740px; border-radius:44px; background:{c['bg']}; box-shadow:0 30px 60px rgba(0,0,0,.25);
           border:10px solid #111; box-sizing:border-box; display:flex; flex-direction:column; align-items:center;
           justify-content:center; padding:32px 28px; text-align:center; color:{c['ink']}; }}
  .brand {{ font-weight:700; letter-spacing:.4px; color:{c['gold']}; margin-top:18px; }}
  h1 {{ font-size:26px; margin:22px 0 10px; }}
  p {{ color:{c['mid']}; line-height:1.55; font-size:15px; margin:0; }}
  .eta {{ margin-top:18px; background:{c['surface']}; border:1px solid {c['gold']}55; border-radius:14px; padding:12px 14px; font-size:14px; }}
  button {{ width:100%; height:52px; border-radius:16px; font-size:16px; font-weight:700; margin-top:12px; cursor:pointer; }}
  .primary {{ background:{c['purple']}; color:{c['on']}; border:none; margin-top:28px; }}
  .secondary {{ background:transparent; color:{c['ink']}; border:1px solid {c['gold']}; }}
</style></head>
<body>
<div class="state">Maintenance mode is <b>{state}</b> · theme: {escape(theme)} · time zone: {escape(tz_name)}</div>
<div class="phone">
  <svg width="150" height="150" viewBox="0 0 150 150" aria-hidden="true">
    <circle cx="75" cy="75" r="70" fill="{c['gold']}" opacity=".14"/>
    <circle cx="75" cy="75" r="50" fill="none" stroke="{c['gold']}" stroke-width="3" opacity=".55"/>
    <path d="M75 38 L75 75 L98 88" stroke="{c['purple']}" stroke-width="6" stroke-linecap="round" fill="none"/>
    <circle cx="75" cy="75" r="6" fill="{c['gold']}"/>
  </svg>
  <div class="brand">WisdomOne</div>
  <h1>{title}</h1>
  <p>{message}</p>
  {eta_html}
  <button class="primary">Close App</button>
  {retry}
</div>
</body></html>"""
