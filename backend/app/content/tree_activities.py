"""Wisdom Tree activity catalogue: five monthly levels (150-day journey), each with its own unique activities.

``metric`` says how progress is measured *inside the current level's window* (from the day the level
began):

* ``reading_days``    distinct days with the daily reading completed (counted from the tree's reading history)
* ``favorites``       verses saved
* ``mood_checkins``   mood check-ins recorded
* ``reflections``     Today's Reflection entries saved
* ``challenge_days``  challenge days completed
* any other name      a client-reported activity event (see ``ACTIVITY_EVENTS``) counted as ``count`` events
                      or as distinct ``days`` with the event, per ``mode``

No activity code repeats across levels (enforced by a unit test).
"""

ACTIVITY_EVENTS = {"explain", "share", "story", "studio", "truths", "wallpaper", "widget", "discover"}

LEVELS: list[dict] = [
    {"level": 1, "title": "Planting Seeds", "days": "Days 1–30", "subtitle": "Build the habit of reading and reflecting.",
     "activities": [
         {"code": "l1_read", "title": "Read your daily verse", "description": "Complete the daily reading on 7 different days.", "metric": "reading_days", "target": 7},
         {"code": "l1_save", "title": "Save verses you love", "description": "Save 3 verses to your collection.", "metric": "favorites", "target": 3},
         {"code": "l1_mood", "title": "Check in with your mood", "description": "Record 3 mood check-ins.", "metric": "mood_checkins", "target": 3},
         {"code": "l1_explain", "title": "Understand simply", "description": "Open “Explain this like I’m 5” on 2 verses.", "metric": "explain", "target": 2},
         {"code": "l1_reflect", "title": "Your first reflection", "description": "Write and save your first Today’s Reflection.", "metric": "reflections", "target": 1},
         {"code": "l1_share", "title": "Spread the light", "description": "Share a verse with someone.", "metric": "share", "target": 1},
     ]},
    {"level": 2, "title": "Growing Roots", "days": "Days 31–60", "subtitle": "Go deeper into stories, themes and challenges.",
     "activities": [
         {"code": "l2_read", "title": "Stay steady", "description": "Complete the daily reading on 15 different days.", "metric": "reading_days", "target": 15},
         {"code": "l2_reflect", "title": "Reflect regularly", "description": "Save 5 reflections.", "metric": "reflections", "target": 5},
         {"code": "l2_challenge", "title": "Take on a challenge", "description": "Complete 3 challenge days.", "metric": "challenge_days", "target": 3},
         {"code": "l2_story", "title": "Story time", "description": "Read 2 stories from the story library.", "metric": "story", "target": 2},
         {"code": "l2_truths", "title": "Universal truths", "description": "Explore 2 Universal Truth topics.", "metric": "truths", "target": 2},
         {"code": "l2_collect", "title": "Grow your collection", "description": "Save 5 more verses.", "metric": "favorites", "target": 5},
     ]},
    {"level": 3, "title": "Reaching Branches", "days": "Days 61–90", "subtitle": "Share and create with what you have learned.",
     "activities": [
         {"code": "l3_read", "title": "Reading rhythm", "description": "Complete the daily reading on 22 different days.", "metric": "reading_days", "target": 22},
         {"code": "l3_reflect", "title": "Honest reflections", "description": "Save 10 reflections.", "metric": "reflections", "target": 10},
         {"code": "l3_share", "title": "Share on 5 days", "description": "Share verses on 5 different days.", "metric": "share", "target": 5, "mode": "days"},
         {"code": "l3_studio", "title": "Create in Studio", "description": "Create 2 verse images or videos in Studio.", "metric": "studio", "target": 2},
         {"code": "l3_challenge", "title": "A week of practice", "description": "Complete 7 challenge days.", "metric": "challenge_days", "target": 7},
         {"code": "l3_mood", "title": "Know your heart", "description": "Record 7 mood check-ins.", "metric": "mood_checkins", "target": 7},
     ]},
    {"level": 4, "title": "Blooming Flowers", "days": "Days 91–120", "subtitle": "Make WisdomOne part of your everyday surroundings.",
     "activities": [
         {"code": "l4_read", "title": "Almost every day", "description": "Complete the daily reading on 26 different days.", "metric": "reading_days", "target": 26},
         {"code": "l4_reflect", "title": "Deepen your journal", "description": "Save 15 reflections.", "metric": "reflections", "target": 15},
         {"code": "l4_wallpaper", "title": "Verse on your screen", "description": "Apply a verse wallpaper to your device.", "metric": "wallpaper", "target": 1},
         {"code": "l4_widget", "title": "Verse on your home", "description": "Customise a home-screen widget.", "metric": "widget", "target": 1},
         {"code": "l4_discover", "title": "Discover new wisdom", "description": "Discover 5 new verses.", "metric": "discover", "target": 5},
         {"code": "l4_explain", "title": "Explain it simply", "description": "Open the simple explanation on 10 verses.", "metric": "explain", "target": 10},
     ]},
    {"level": 5, "title": "Harvest of Wisdom", "days": "Days 121–150", "subtitle": "Live what you have learned and pass it on.",
     "activities": [
         {"code": "l5_read", "title": "A devoted month", "description": "Complete the daily reading on 30 different days.", "metric": "reading_days", "target": 30},
         {"code": "l5_reflect", "title": "Twenty reflections", "description": "Save 20 reflections.", "metric": "reflections", "target": 20},
         {"code": "l5_share", "title": "Light for others", "description": "Share verses on 10 different days.", "metric": "share", "target": 10, "mode": "days"},
         {"code": "l5_challenge", "title": "Three weeks of change", "description": "Complete 21 challenge days.", "metric": "challenge_days", "target": 21},
         {"code": "l5_story", "title": "Storyteller", "description": "Read 5 stories.", "metric": "story", "target": 5},
         {"code": "l5_collect", "title": "A treasury of verses", "description": "Save 15 more verses.", "metric": "favorites", "target": 15},
     ]},
]


def level_catalog(level: int) -> dict:
    return LEVELS[max(1, min(level, len(LEVELS))) - 1]
