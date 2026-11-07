# Home layout after Wisdom Tree completion

When the user completes 150 distinct reading days (five levels × 30 days), Home receives no Wisdom Tree composable at all (`treeSlot = null`). The Home column does not render a placeholder, fixed-height container, spacer, or completion banner in its place. The next Home section is laid out immediately after the preceding section and the scroll content naturally moves upward.

The reading history and completed journey remain preserved. The tree card is not restarted automatically.

Implementation:
- `HomeScreen` now accepts a nullable composable tree slot.
- The tree slot is invoked only when non-null.
- The caller passes `null` after journey completion, otherwise passes the Wisdom Tree card.

This change is limited to Home layout composition; other Home sections retain their existing order and styling.
