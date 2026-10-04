from sqlalchemy import select
from sqlalchemy.orm import Session
from app.models.entities import Religion, Language, Scripture, Verse

# These are original illustrative paraphrases, not canonical translations. Replace with licensed text before publishing.
CONTENT = [
    # Bhagavad Gita
    ("gita", "en", "Bhagavad Gita", "Hinduism", 2, 47, "Give your attention to the work before you; let go of claiming control over its results."),
    ("gita", "hi", "Bhagavad Gita", "Hinduism", 2, 47, "अपने कर्तव्य पर ध्यान दें; उसके फल पर अपना अधिकार न मानें।"),
    ("gita", "en", "Bhagavad Gita", "Hinduism", 2, 48, "Act with equanimity; sameness in success and failure is called yoga."),
    ("gita", "hi", "Bhagavad Gita", "Hinduism", 2, 48, "समभाव से कार्य करें; सफलता और विफलता में समानता ही योग है।"),
    ("gita", "en", "Bhagavad Gita", "Hinduism", 3, 19, "Therefore, always do what must be done without attachment; through detached action one reaches the highest."),
    ("gita", "hi", "Bhagavad Gita", "Hinduism", 3, 19, "इसलिए आसक्ति रहित होकर सदा कर्तव्य कर्म करें; अनासक्त कर्म से परम को प्राप्त होते हैं।"),
    ("gita", "en", "Bhagavad Gita", "Hinduism", 4, 7, "Whenever righteousness declines and unrighteousness rises, I manifest myself."),
    ("gita", "hi", "Bhagavad Gita", "Hinduism", 4, 7, "जब-जब धर्म की हानि होती है और अधर्म बढ़ता है, तब-तब मैं प्रकट होता हूँ।"),
    ("gita", "en", "Bhagavad Gita", "Hinduism", 6, 5, "With steady effort, let the mind become a support for your own growth."),
    ("gita", "hi", "Bhagavad Gita", "Hinduism", 6, 5, "स्थिर प्रयास से मन को अपने उत्थान का सहायक बनाइए।"),
    ("gita", "en", "Bhagavad Gita", "Hinduism", 9, 22, "To those who worship me with devotion, I carry what they lack and preserve what they have."),
    ("gita", "hi", "Bhagavad Gita", "Hinduism", 9, 22, "जो भक्तिपूर्वक मेरी उपासना करते हैं, उनकी कमी मैं पूरी करता हूँ और जो है उसे सुरक्षित रखता हूँ।"),
    # Bible
    ("bible", "en", "Bible", "Christianity", 1, 1, "Let your actions be guided by love, patience, and hope."),
    ("bible", "hi", "Bible", "Christianity", 1, 1, "अपने आचरण को प्रेम, धैर्य और आशा से दिशा दें।"),
    ("bible", "en", "Bible", "Christianity", 1, 2, "Trust in the Lord with all your heart and lean not on your own understanding."),
    ("bible", "hi", "Bible", "Christianity", 1, 2, "पूरे मन से प्रभु पर भरोसा रखें और अपनी समझ पर निर्भर न रहें।"),
    ("bible", "en", "Bible", "Christianity", 2, 1, "Seek peace in what you can do today, and meet others with compassion."),
    ("bible", "hi", "Bible", "Christianity", 2, 1, "आज जो कर सकते हैं उसमें शांति खोजें और दूसरों से करुणा से मिलें।"),
    ("bible", "en", "Bible", "Christianity", 2, 2, "Be kind to one another, tenderhearted, forgiving one another as you have been forgiven."),
    ("bible", "hi", "Bible", "Christianity", 2, 2, "एक दूसरे के प्रति दयालु और कोमल हृदय रखें, जैसे आपको क्षमा मिली है वैसे क्षमा करें।"),
    ("bible", "en", "Bible", "Christianity", 3, 1, "Do not be anxious about anything; in every situation present your requests to God with thanksgiving."),
    ("bible", "hi", "Bible", "Christianity", 3, 1, "किसी भी बात की चिंता न करें; हर परिस्थिति में कृतज्ञता के साथ परमेश्वर से विनती करें।"),
    ("bible", "en", "Bible", "Christianity", 3, 2, "The fruit of the Spirit is love, joy, peace, patience, kindness, goodness, and faithfulness."),
    ("bible", "hi", "Bible", "Christianity", 3, 2, "आत्मा का फल प्रेम, आनंद, शांति, धैर्य, दयालुता, भलाई और विश्वासयोग्यता है।"),
    # Quran
    ("quran", "en", "Quran", "Islam", 1, 1, "Begin with gratitude and seek the path of mercy and wisdom."),
    ("quran", "hi", "Quran", "Islam", 1, 1, "कृतज्ञता से आरंभ करें और दया तथा विवेक का मार्ग खोजें।"),
    ("quran", "en", "Quran", "Islam", 1, 2, "All praise belongs to the Lord of all worlds, the Most Gracious, the Most Merciful."),
    ("quran", "hi", "Quran", "Islam", 1, 2, "सारी प्रशंसा सभी जगत के पालनहार को है, जो अत्यंत कृपालु और दयावान है।"),
    ("quran", "en", "Quran", "Islam", 2, 1, "Choose patience and kindness, especially when the way forward is difficult."),
    ("quran", "hi", "Quran", "Islam", 2, 1, "जब आगे का मार्ग कठिन हो, तब भी धैर्य और दयालुता चुनें।"),
    ("quran", "en", "Quran", "Islam", 2, 2, "Verily, with hardship comes ease; do not lose hope in the mercy of God."),
    ("quran", "hi", "Quran", "Islam", 2, 2, "निश्चय ही कठिनाई के साथ आसानी है; ईश्वर की दया से निराश न हों।"),
    ("quran", "en", "Quran", "Islam", 3, 1, "Speak good words or remain silent; the tongue is a trust."),
    ("quran", "hi", "Quran", "Islam", 3, 1, "अच्छी बात कहें या चुप रहें; जीभ एक अमानत है।"),
    ("quran", "en", "Quran", "Islam", 3, 2, "God does not burden a soul beyond what it can bear."),
    ("quran", "hi", "Quran", "Islam", 3, 2, "ईश्वर किसी आत्मा पर उसकी सामर्थ्य से अधिक बोझ नहीं डालता।"),
    # Dhammapada
    ("dhammapada", "en", "Dhammapada", "Buddhism", 1, 1, "Mind is the forerunner of all actions; what we think, we become."),
    ("dhammapada", "hi", "Dhammapada", "Buddhism", 1, 1, "मन सभी कार्यों का अग्रदूत है; हम जो सोचते हैं, वही बन जाते हैं।"),
    ("dhammapada", "en", "Dhammapada", "Buddhism", 1, 2, "Hatred is never appeased by hatred; it is appeased by love alone."),
    ("dhammapada", "hi", "Dhammapada", "Buddhism", 1, 2, "घृणा से घृणा कभी शांत नहीं होती; वह केवल प्रेम से शांत होती है।"),
    ("dhammapada", "en", "Dhammapada", "Buddhism", 2, 1, "Better than a thousand hollow words is one word that brings peace."),
    ("dhammapada", "hi", "Dhammapada", "Buddhism", 2, 1, "हजार खोखले शब्दों से बेहतर एक ऐसा शब्द है जो शांति लाए।"),
    ("dhammapada", "en", "Dhammapada", "Buddhism", 2, 2, "Conquer anger with non-anger, evil with good, and the miser with generosity."),
    ("dhammapada", "hi", "Dhammapada", "Buddhism", 2, 2, "क्रोध को अक्रोध से, बुराई को भलाई से और कंजूस को उदारता से जीतें।"),
    ("dhammapada", "en", "Dhammapada", "Buddhism", 3, 1, "Do not dwell in the past, do not dream of the future; concentrate the mind on the present moment."),
    ("dhammapada", "hi", "Dhammapada", "Buddhism", 3, 1, "अतीत में मत रहो, भविष्य के सपने मत देखो; मन को वर्तमान क्षण पर केंद्रित करो।"),
]


def seed_database(db: Session) -> None:
    religions = [("Hinduism", "hinduism"), ("Christianity", "christianity"), ("Islam", "islam"), ("Buddhism", "buddhism")]
    languages = [("English", "en"), ("Hindi", "hi")]
    for name, code in religions:
        if not db.scalar(select(Religion).where(Religion.code == code)):
            db.add(Religion(name=name, code=code))
    for name, code in languages:
        if not db.scalar(select(Language).where(Language.code == code)):
            db.add(Language(name=name, code=code))
    db.flush()
    religion_map = {r.code: r for r in db.scalars(select(Religion)).all()}
    language_map = {l.code: l for l in db.scalars(select(Language)).all()}
    scriptures = {}
    for code, _, name, religion, *_ in CONTENT:
        if code not in scriptures:
            row = db.scalar(select(Scripture).where(Scripture.code == code))
            if row is None:
                row = Scripture(name=name, code=code, religion_id=religion_map[religion.lower()].id)
                db.add(row)
                db.flush()
            scriptures[code] = row
    for scripture_code, language_code, _, _, chapter, number, text in CONTENT:
        existing = db.scalar(select(Verse).where(Verse.scripture_id == scriptures[scripture_code].id, Verse.language_id == language_map[language_code].id, Verse.chapter == chapter, Verse.verse_number == number))
        if existing is None:
            db.add(Verse(scripture_id=scriptures[scripture_code].id, language_id=language_map[language_code].id,
                         book=None, chapter=chapter, verse_number=number, text=text,
                         translation="Original illustrative paraphrase — not a canonical translation.",
                         source="Scripture Daily demo content", license="Original demo paraphrase; replace with licensed content", is_demo=True))
    db.commit()
