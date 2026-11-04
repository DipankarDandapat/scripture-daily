"""Share Studio / Video Studio template catalogue.

Row: (code, name, kind, tradition, motif, top, bottom, text, accent, muted, is_light, emoji)

* ``tradition`` None  -> common template, shown to every user.
* ``tradition`` code  -> shown only to users who selected that tradition (``general wisdom`` templates
  carry no religious imagery: nature, gratitude, peace and universal wisdom).
* ``motif`` is the vector illustration the app draws behind the verse. No licensed imagery is shipped.
The table is synced into ``studio_templates``; edit rows there to manage templates without a release.
"""

COMMON = [
    ("midnight", "Midnight", "both", None, "mandala", "#0F0A1A", "#2D1B4E", "#F5F0E8", "#D4A574", "#B5A8C4", False, "🌙"),
    ("sunrise", "Sunrise", "both", None, "rays", "#F6A55F", "#FCE3B4", "#3B2314", "#8A4B14", "#6B4A33", True, "🌅"),
    ("forest", "Forest", "both", None, "leaves", "#0E2A22", "#1F5A44", "#EEF5EE", "#BFE3A0", "#A9C9B5", False, "🌲"),
    ("ocean", "Ocean", "both", None, "waves", "#0A2540", "#1E6F9F", "#FFFFFF", "#9FE1FF", "#B8D8EA", False, "🌊"),
    ("rose", "Rose", "both", None, "bokeh", "#3A1426", "#A24A6A", "#FFF1F4", "#FFC4D2", "#E6B9C5", False, "🌸"),
    ("ivory", "Ivory", "both", None, "none", "#FBF6EC", "#EADFC8", "#2A1F14", "#9A6B2F", "#6F5B45", True, "📜"),
    ("slate", "Slate", "both", None, "none", "#1C2430", "#3A4A5E", "#F1F4F8", "#9CC3E8", "#B3BFCC", False, "🪨"),
    ("lavender", "Lavender mist", "both", None, "bokeh", "#EDE7F6", "#C9B8E8", "#2B1F45", "#5E3FA3", "#5B4D7A", True, "💜"),
]

HINDUISM = [
    ("om-saffron", "Om", "both", "hinduism", "om", "#2A0E05", "#8A3A0B", "#FFF3E0", "#FFC857", "#E3B98A", False, "🕉️"),
    ("krishna-peacock", "Krishna · Peacock feather", "both", "hinduism", "peacock", "#0A2F3A", "#1B6B6B", "#F2FBF8", "#7FE0C8", "#A8D5D0", False, "🪈"),
    ("radha-lotus", "Radha · Lotus", "both", "hinduism", "lotus", "#4A1530", "#C0557F", "#FFF1F5", "#FFD1DC", "#F0BCCB", False, "🪷"),
    ("shiva-moon", "Shiva · Trident & moon", "both", "hinduism", "trident", "#0B1021", "#2B3A67", "#EEF1FA", "#B8C4FF", "#AAB4D6", False, "🔱"),
    ("ganesha-diya", "Ganesha · Diya light", "both", "hinduism", "diya", "#3A1A00", "#B5651D", "#FFF6E5", "#FFD27A", "#F0CFA0", False, "🪔"),
    ("gita-sun", "Bhagavad Gita · Dawn", "both", "hinduism", "mandala", "#5B2A0A", "#F2A65A", "#2B1405", "#7A3410", "#5A3A22", True, "☀️"),
]

CHRISTIANITY = [
    ("cross-light", "Cross of light", "both", "christianity", "cross", "#14213D", "#3E5C9A", "#F4F7FF", "#F2D388", "#B8C6E6", False, "✝️"),
    ("dove-peace", "Dove of peace", "both", "christianity", "dove", "#E8F1FB", "#BFD6EE", "#17304F", "#3A6EA5", "#456B94", True, "🕊️"),
    ("olive-branch", "Olive branch", "both", "christianity", "olive", "#1D3A2A", "#587A4B", "#F6F8EE", "#E6D88F", "#C2CFAE", False, "🌿"),
    ("stained-glass", "Church window", "both", "christianity", "stained", "#2A1240", "#7A3E8E", "#FFF7FB", "#FFD479", "#E0BEE6", False, "⛪"),
    ("bible-parchment", "Scripture parchment", "both", "christianity", "cross", "#F4E8CF", "#E1CD9E", "#2F2210", "#7A4E16", "#5E4A2B", True, "📖"),
]

ISLAM = [
    ("geometry-emerald", "Geometric emerald", "both", "islam", "geometry", "#06302A", "#0F6B57", "#F0FBF6", "#E3C468", "#A9D0C4", False, "🕌"),
    ("crescent-night", "Crescent night", "both", "islam", "crescent", "#0B1B3A", "#1D3C6E", "#F2F5FC", "#F5D77A", "#AEBBD8", False, "🌙"),
    ("mosque-dusk", "Mosque at dusk", "both", "islam", "mosque", "#3A2A52", "#D9885B", "#FFF8EF", "#FFE0A3", "#F2D2BC", False, "🕌"),
    ("arabesque-gold", "Arabesque gold", "both", "islam", "arabesque", "#1F1608", "#5C4318", "#FFF6DF", "#F0C75E", "#D8C192", False, "✨"),
    ("ivory-geometry", "Ivory geometry", "both", "islam", "geometry", "#F7F1E3", "#E6D8B5", "#2C2415", "#0F6B57", "#5E5236", True, "🔷"),
]

BUDDHISM = [
    ("lotus-calm", "Lotus calm", "both", "buddhism", "lotus", "#2C1B3F", "#8E4A7E", "#FFF2FA", "#FFD1EA", "#E3BCD6", False, "🪷"),
    ("dharma-wheel", "Dharma wheel", "both", "buddhism", "dharma", "#3B1305", "#A4601A", "#FFF4E2", "#FFD27A", "#EBCB9E", False, "☸️"),
    ("bodhi-leaf", "Bodhi leaf", "both", "buddhism", "bodhi", "#0F2E1C", "#3C7A4B", "#F0F9F1", "#C8EAA0", "#A9CDB0", False, "🍃"),
    ("meditation-dawn", "Meditation dawn", "both", "buddhism", "halo", "#F9D9A8", "#F6A85B", "#3A2210", "#8A4A10", "#6B4A2A", True, "🧘"),
]

GENERAL = [
    ("nature-mountains", "Mountains", "both", "general wisdom", "mountains", "#1B3A4B", "#6C8EAD", "#F6FAFD", "#FFE2A8", "#C3D3E2", False, "⛰️"),
    ("gratitude-sunrise", "Gratitude sunrise", "both", "general wisdom", "sunrise", "#F7B267", "#FFE3B8", "#3A2112", "#9A4A12", "#6B4A33", True, "🌄"),
    ("peace-leaves", "Quiet leaves", "both", "general wisdom", "leaves", "#193D2E", "#4C8C63", "#F3FAF4", "#D6EFA4", "#B3D2BC", False, "🍃"),
    ("calm-waves", "Calm waves", "both", "general wisdom", "waves", "#0D3050", "#3A8CBB", "#FFFFFF", "#BFEBFF", "#C5DDEC", False, "🌊"),
    ("universal-stars", "Universal stars", "both", "general wisdom", "stars", "#0B0D24", "#2B2A66", "#F4F2FF", "#FFD98A", "#B9B6DC", False, "✨"),
]

TEMPLATES = COMMON + HINDUISM + CHRISTIANITY + ISLAM + BUDDHISM + GENERAL
