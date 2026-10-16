from sqlalchemy import select
from sqlalchemy.orm import Session
from app.models.entities import Religion, Language, ReligionLanguage, Scripture, Verse

# Original illustrative paraphrases — not canonical translations. Replace with licensed text before publishing.

CONTENT = [
    # ── Bhagavad Gita (Hinduism) ──────────────────────────────────────────────
    ("gita", "en", "Bhagavad Gita", "Hinduism", 2, 47, "Give your attention to the work before you; let go of claiming control over its results."),
    ("gita", "hi", "Bhagavad Gita", "Hinduism", 2, 47, "अपने कर्तव्य पर ध्यान दें; उसके फल पर अपना अधिकार न मानें।"),
    ("gita", "bn", "Bhagavad Gita", "Hinduism", 2, 47, "তোমার সামনের কাজে মনোযোগ দাও; ফলের উপর দাবি ছেড়ে দাও।"),
    ("gita", "en", "Bhagavad Gita", "Hinduism", 2, 48, "Act with equanimity; sameness in success and failure is called yoga."),
    ("gita", "hi", "Bhagavad Gita", "Hinduism", 2, 48, "समभाव से कार्य करें; सफलता और विफलता में समानता ही योग है।"),
    ("gita", "bn", "Bhagavad Gita", "Hinduism", 2, 48, "সমতার সাথে কাজ করো; সাফল্য ও ব্যর্থতায় সমতাই যোগ।"),
    ("gita", "en", "Bhagavad Gita", "Hinduism", 3, 19, "Therefore, always do what must be done without attachment; through detached action one reaches the highest."),
    ("gita", "hi", "Bhagavad Gita", "Hinduism", 3, 19, "इसलिए आसक्ति रहित होकर सदा कर्तव्य कर्म करें; अनासक्त कर्म से परम को प्राप्त होते हैं।"),
    ("gita", "bn", "Bhagavad Gita", "Hinduism", 3, 19, "তাই আসক্তি ছাড়া সর্বদা কর্তব্য করো; অনাসক্ত কর্মের মাধ্যমে সর্বোচ্চে পৌঁছানো যায়।"),
    ("gita", "en", "Bhagavad Gita", "Hinduism", 4, 7, "Whenever righteousness declines and unrighteousness rises, I manifest myself."),
    ("gita", "hi", "Bhagavad Gita", "Hinduism", 4, 7, "जब-जब धर्म की हानि होती है और अधर्म बढ़ता है, तब-तब मैं प्रकट होता हूँ।"),
    ("gita", "bn", "Bhagavad Gita", "Hinduism", 4, 7, "যখনই ধর্মের অবক্ষয় হয় এবং অধর্ম বাড়ে, তখনই আমি আবির্ভূত হই।"),
    ("gita", "en", "Bhagavad Gita", "Hinduism", 6, 5, "With steady effort, let the mind become a support for your own growth."),
    ("gita", "hi", "Bhagavad Gita", "Hinduism", 6, 5, "स्थिर प्रयास से मन को अपने उत्थान का सहायक बनाइए।"),
    ("gita", "bn", "Bhagavad Gita", "Hinduism", 6, 5, "স্থির প্রচেষ্টায় মনকে নিজের উন্নতির সহায়ক করো।"),

    # ── Ramayana (Hinduism) ───────────────────────────────────────────────────
    ("ramayana", "en", "Ramayana", "Hinduism", 1, 1, "Righteousness is the foundation of all; one who walks the path of dharma is never forsaken."),
    ("ramayana", "hi", "Ramayana", "Hinduism", 1, 1, "धर्म ही सबका आधार है; जो धर्म के मार्ग पर चलता है, वह कभी नहीं छोड़ा जाता।"),
    ("ramayana", "bn", "Ramayana", "Hinduism", 1, 1, "ধর্মই সবকিছুর ভিত্তি; যে ধর্মের পথে চলে সে কখনো পরিত্যক্ত হয় না।"),
    ("ramayana", "en", "Ramayana", "Hinduism", 1, 2, "A person's true character is revealed not in comfort, but in the face of adversity."),
    ("ramayana", "hi", "Ramayana", "Hinduism", 1, 2, "व्यक्ति का सच्चा चरित्र सुख में नहीं, बल्कि विपत्ति में प्रकट होता है।"),
    ("ramayana", "bn", "Ramayana", "Hinduism", 1, 2, "মানুষের প্রকৃত চরিত্র সুখে নয়, বিপদের মুখে প্রকাশ পায়।"),
    ("ramayana", "en", "Ramayana", "Hinduism", 2, 1, "Serve your parents with devotion; there is no greater act of worship in this world."),
    ("ramayana", "hi", "Ramayana", "Hinduism", 2, 1, "माता-पिता की भक्तिपूर्वक सेवा करें; इस संसार में इससे बड़ी कोई पूजा नहीं है।"),
    ("ramayana", "bn", "Ramayana", "Hinduism", 2, 1, "ভক্তির সাথে পিতামাতার সেবা করো; এই জগতে এর চেয়ে বড় পূজা নেই।"),
    ("ramayana", "en", "Ramayana", "Hinduism", 2, 2, "Patience and perseverance are the greatest virtues; they lead even the impossible to completion."),
    ("ramayana", "hi", "Ramayana", "Hinduism", 2, 2, "धैर्य और दृढ़ता सबसे बड़े गुण हैं; वे असंभव को भी पूर्ण करा देते हैं।"),
    ("ramayana", "bn", "Ramayana", "Hinduism", 2, 2, "ধৈর্য ও অধ্যবসায় সর্বোচ্চ গুণ; এগুলো অসম্ভবকেও সম্পন্ন করে।"),
    ("ramayana", "en", "Ramayana", "Hinduism", 3, 1, "Truth alone triumphs; falsehood may seem to prosper for a while, but it cannot endure."),
    ("ramayana", "hi", "Ramayana", "Hinduism", 3, 1, "सत्य की ही विजय होती है; असत्य कुछ समय के लिए फल-फूल सकता है, पर टिक नहीं सकता।"),
    ("ramayana", "bn", "Ramayana", "Hinduism", 3, 1, "সত্যেরই জয় হয়; মিথ্যা কিছুকাল সমৃদ্ধ মনে হলেও টিকতে পারে না।"),

    # ── Mahabharata (Hinduism) ────────────────────────────────────────────────
    ("mahabharata", "en", "Mahabharata", "Hinduism", 1, 1, "Do not do to others what you would not wish done to yourself; this is the whole of dharma."),
    ("mahabharata", "hi", "Mahabharata", "Hinduism", 1, 1, "दूसरों के साथ वह मत करो जो तुम अपने साथ नहीं चाहते; यही संपूर्ण धर्म है।"),
    ("mahabharata", "bn", "Mahabharata", "Hinduism", 1, 1, "অন্যের সাথে তা করো না যা তুমি নিজের সাথে চাও না; এটাই সমগ্র ধর্ম।"),
    ("mahabharata", "en", "Mahabharata", "Hinduism", 1, 2, "Time is the root of all things; it is time that ripens and destroys all creatures."),
    ("mahabharata", "hi", "Mahabharata", "Hinduism", 1, 2, "काल ही सब वस्तुओं की जड़ है; काल ही सभी प्राणियों को पकाता और नष्ट करता है।"),
    ("mahabharata", "bn", "Mahabharata", "Hinduism", 1, 2, "কালই সব কিছুর মূল; কালই সকল প্রাণীকে পরিপক্ব করে এবং ধ্বংস করে।"),
    ("mahabharata", "en", "Mahabharata", "Hinduism", 2, 1, "Contentment is the highest gain; good company is the highest pleasure; wisdom is the highest power."),
    ("mahabharata", "hi", "Mahabharata", "Hinduism", 2, 1, "संतोष सबसे बड़ा लाभ है; सत्संग सबसे बड़ा सुख है; विवेक सबसे बड़ी शक्ति है।"),
    ("mahabharata", "bn", "Mahabharata", "Hinduism", 2, 1, "সন্তুষ্টি সর্বোচ্চ লাভ; সৎসঙ্গ সর্বোচ্চ আনন্দ; বিবেক সর্বোচ্চ শক্তি।"),
    ("mahabharata", "en", "Mahabharata", "Hinduism", 2, 2, "Forgiveness is the greatest virtue; it is the strength of the strong, not the weakness of the weak."),
    ("mahabharata", "hi", "Mahabharata", "Hinduism", 2, 2, "क्षमा सबसे बड़ा गुण है; यह बलवानों की शक्ति है, कमजोरों की कमजोरी नहीं।"),
    ("mahabharata", "bn", "Mahabharata", "Hinduism", 2, 2, "ক্ষমা সর্বোচ্চ গুণ; এটি শক্তিশালীদের শক্তি, দুর্বলদের দুর্বলতা নয়।"),
    ("mahabharata", "en", "Mahabharata", "Hinduism", 3, 1, "One should never abandon a friend in times of danger; loyalty is the mark of a noble soul."),
    ("mahabharata", "hi", "Mahabharata", "Hinduism", 3, 1, "संकट के समय मित्र को कभी नहीं छोड़ना चाहिए; निष्ठा ही उदार आत्मा की पहचान है।"),
    ("mahabharata", "bn", "Mahabharata", "Hinduism", 3, 1, "বিপদের সময় বন্ধুকে কখনো ছেড়ে যাওয়া উচিত নয়; বিশ্বস্ততাই মহৎ আত্মার পরিচয়।"),

    # ── Bible (Christianity) ──────────────────────────────────────────────────
    ("bible", "en", "Bible", "Christianity", 1, 1, "Let your actions be guided by love, patience, and hope."),
    ("bible", "hi", "Bible", "Christianity", 1, 1, "अपने आचरण को प्रेम, धैर्य और आशा से दिशा दें।"),
    ("bible", "es", "Bible", "Christianity", 1, 1, "Que tus acciones estén guiadas por el amor, la paciencia y la esperanza."),
    ("bible", "en", "Bible", "Christianity", 1, 2, "Trust in the Lord with all your heart and lean not on your own understanding."),
    ("bible", "hi", "Bible", "Christianity", 1, 2, "पूरे मन से प्रभु पर भरोसा रखें और अपनी समझ पर निर्भर न रहें।"),
    ("bible", "es", "Bible", "Christianity", 1, 2, "Confía en el Señor con todo tu corazón y no te apoyes en tu propio entendimiento."),
    ("bible", "en", "Bible", "Christianity", 2, 1, "Seek peace in what you can do today, and meet others with compassion."),
    ("bible", "hi", "Bible", "Christianity", 2, 1, "आज जो कर सकते हैं उसमें शांति खोजें और दूसरों से करुणा से मिलें।"),
    ("bible", "es", "Bible", "Christianity", 2, 1, "Busca la paz en lo que puedes hacer hoy y encuentra a los demás con compasión."),
    ("bible", "en", "Bible", "Christianity", 2, 2, "Be kind to one another, tenderhearted, forgiving one another as you have been forgiven."),
    ("bible", "hi", "Bible", "Christianity", 2, 2, "एक दूसरे के प्रति दयालु और कोमल हृदय रखें, जैसे आपको क्षमा मिली है वैसे क्षमा करें।"),
    ("bible", "es", "Bible", "Christianity", 2, 2, "Sean bondadosos y compasivos unos con otros, perdonándose mutuamente como Dios los perdonó."),
    ("bible", "en", "Bible", "Christianity", 3, 1, "Do not be anxious about anything; in every situation present your requests to God with thanksgiving."),
    ("bible", "hi", "Bible", "Christianity", 3, 1, "किसी भी बात की चिंता न करें; हर परिस्थिति में कृतज्ञता के साथ परमेश्वर से विनती करें।"),
    ("bible", "es", "Bible", "Christianity", 3, 1, "No se inquieten por nada; en toda situación presenten sus peticiones a Dios con acción de gracias."),

    # ── Quran (Islam) ─────────────────────────────────────────────────────────
    ("quran", "en", "Quran", "Islam", 1, 1, "Begin with gratitude and seek the path of mercy and wisdom."),
    ("quran", "hi", "Quran", "Islam", 1, 1, "कृतज्ञता से आरंभ करें और दया तथा विवेक का मार्ग खोजें।"),
    ("quran", "ar", "Quran", "Islam", 1, 1, "ابدأ بالشكر وابحث عن طريق الرحمة والحكمة."),
    ("quran", "ur", "Quran", "Islam", 1, 1, "شکر کے ساتھ آغاز کریں اور رحمت و حکمت کا راستہ تلاش کریں۔"),
    ("quran", "en", "Quran", "Islam", 1, 2, "All praise belongs to the Lord of all worlds, the Most Gracious, the Most Merciful."),
    ("quran", "hi", "Quran", "Islam", 1, 2, "सारी प्रशंसा सभी जगत के पालनहार को है, जो अत्यंत कृपालु और दयावान है।"),
    ("quran", "ar", "Quran", "Islam", 1, 2, "الحمد لله رب العالمين، الرحمن الرحيم."),
    ("quran", "ur", "Quran", "Islam", 1, 2, "تمام تعریفیں اللہ کے لیے ہیں جو تمام جہانوں کا رب ہے، بے حد مہربان اور رحم کرنے والا۔"),
    ("quran", "en", "Quran", "Islam", 2, 1, "Choose patience and kindness, especially when the way forward is difficult."),
    ("quran", "hi", "Quran", "Islam", 2, 1, "जब आगे का मार्ग कठिन हो, तब भी धैर्य और दयालुता चुनें।"),
    ("quran", "ar", "Quran", "Islam", 2, 1, "اختر الصبر واللطف، خاصة حين يكون الطريق صعباً."),
    ("quran", "ur", "Quran", "Islam", 2, 1, "صبر اور مہربانی کا انتخاب کریں، خاص طور پر جب راستہ مشکل ہو۔"),
    ("quran", "en", "Quran", "Islam", 2, 2, "Verily, with hardship comes ease; do not lose hope in the mercy of God."),
    ("quran", "hi", "Quran", "Islam", 2, 2, "निश्चय ही कठिनाई के साथ आसानी है; ईश्वर की दया से निराश न हों।"),
    ("quran", "ar", "Quran", "Islam", 2, 2, "إن مع العسر يسراً؛ لا تيأس من رحمة الله."),
    ("quran", "ur", "Quran", "Islam", 2, 2, "بے شک مشکل کے ساتھ آسانی ہے؛ اللہ کی رحمت سے مایوس نہ ہوں۔"),
    ("quran", "en", "Quran", "Islam", 3, 1, "God does not burden a soul beyond what it can bear."),
    ("quran", "hi", "Quran", "Islam", 3, 1, "ईश्वर किसी आत्मा पर उसकी सामर्थ्य से अधिक बोझ नहीं डालता।"),
    ("quran", "ar", "Quran", "Islam", 3, 1, "لا يكلف الله نفساً إلا وسعها."),
    ("quran", "ur", "Quran", "Islam", 3, 1, "اللہ کسی جان پر اس کی طاقت سے زیادہ بوجھ نہیں ڈالتا۔"),

    # ── Dhammapada (Buddhism) ─────────────────────────────────────────────────
    ("dhammapada", "en", "Dhammapada", "Buddhism", 1, 1, "Mind is the forerunner of all actions; what we think, we become."),
    ("dhammapada", "hi", "Dhammapada", "Buddhism", 1, 1, "मन सभी कार्यों का अग्रदूत है; हम जो सोचते हैं, वही बन जाते हैं।"),
    ("dhammapada", "th", "Dhammapada", "Buddhism", 1, 1, "จิตใจคือผู้นำของการกระทำทั้งหมด สิ่งที่เราคิด เราก็กลายเป็นสิ่งนั้น"),
    ("dhammapada", "en", "Dhammapada", "Buddhism", 1, 2, "Hatred is never appeased by hatred; it is appeased by love alone."),
    ("dhammapada", "hi", "Dhammapada", "Buddhism", 1, 2, "घृणा से घृणा कभी शांत नहीं होती; वह केवल प्रेम से शांत होती है।"),
    ("dhammapada", "th", "Dhammapada", "Buddhism", 1, 2, "ความเกลียดชังไม่เคยสงบด้วยความเกลียดชัง มันสงบได้ด้วยความรักเท่านั้น"),
    ("dhammapada", "en", "Dhammapada", "Buddhism", 2, 1, "Better than a thousand hollow words is one word that brings peace."),
    ("dhammapada", "hi", "Dhammapada", "Buddhism", 2, 1, "हजार खोखले शब्दों से बेहतर एक ऐसा शब्द है जो शांति लाए।"),
    ("dhammapada", "th", "Dhammapada", "Buddhism", 2, 1, "คำพูดที่นำสันติภาพมาหนึ่งคำดีกว่าคำพูดที่ว่างเปล่าพันคำ"),
    ("dhammapada", "en", "Dhammapada", "Buddhism", 2, 2, "Conquer anger with non-anger, evil with good, and the miser with generosity."),
    ("dhammapada", "hi", "Dhammapada", "Buddhism", 2, 2, "क्रोध को अक्रोध से, बुराई को भलाई से और कंजूस को उदारता से जीतें।"),
    ("dhammapada", "th", "Dhammapada", "Buddhism", 2, 2, "ชนะความโกรธด้วยความไม่โกรธ ชนะความชั่วด้วยความดี ชนะความตระหนี่ด้วยความเอื้อเฟื้อ"),
    ("dhammapada", "en", "Dhammapada", "Buddhism", 3, 1, "Do not dwell in the past, do not dream of the future; concentrate the mind on the present moment."),
    ("dhammapada", "hi", "Dhammapada", "Buddhism", 3, 1, "अतीत में मत रहो, भविष्य के सपने मत देखो; मन को वर्तमान क्षण पर केंद्रित करो।"),
    ("dhammapada", "th", "Dhammapada", "Buddhism", 3, 1, "อย่าอยู่กับอดีต อย่าฝันถึงอนาคต จงมุ่งจิตใจไปที่ปัจจุบันขณะ"),

    # ── Inspirational Verses (General Wisdom) ────────────────────────────────
    ("wisdom", "en", "Inspirational Verses", "General Wisdom", 1, 1, "The journey of a thousand miles begins with a single step."),
    ("wisdom", "hi", "Inspirational Verses", "General Wisdom", 1, 1, "हजार मील की यात्रा एक कदम से शुरू होती है।"),
    ("wisdom", "bn", "Inspirational Verses", "General Wisdom", 1, 1, "হাজার মাইলের যাত্রা একটি পদক্ষেপ দিয়ে শুরু হয়।"),
    ("wisdom", "en", "Inspirational Verses", "General Wisdom", 1, 2, "In the middle of every difficulty lies opportunity."),
    ("wisdom", "hi", "Inspirational Verses", "General Wisdom", 1, 2, "हर कठिनाई के बीच में एक अवसर छिपा होता है।"),
    ("wisdom", "bn", "Inspirational Verses", "General Wisdom", 1, 2, "প্রতিটি কঠিনতার মাঝে একটি সুযোগ লুকিয়ে থাকে।"),
    ("wisdom", "en", "Inspirational Verses", "General Wisdom", 2, 1, "Knowing yourself is the beginning of all wisdom."),
    ("wisdom", "hi", "Inspirational Verses", "General Wisdom", 2, 1, "स्वयं को जानना ही सभी ज्ञान की शुरुआत है।"),
    ("wisdom", "bn", "Inspirational Verses", "General Wisdom", 2, 1, "নিজেকে জানাই সমস্ত জ্ঞানের শুরু।"),
    ("wisdom", "en", "Inspirational Verses", "General Wisdom", 2, 2, "The only way to do great work is to love what you do."),
    ("wisdom", "hi", "Inspirational Verses", "General Wisdom", 2, 2, "महान कार्य करने का एकमात्र तरीका यह है कि आप जो करते हैं उससे प्रेम करें।"),
    ("wisdom", "bn", "Inspirational Verses", "General Wisdom", 2, 2, "মহান কাজ করার একমাত্র উপায় হলো তুমি যা করো তা ভালোবাসা।"),
    ("wisdom", "en", "Inspirational Verses", "General Wisdom", 3, 1, "Be the change you wish to see in the world."),
    ("wisdom", "hi", "Inspirational Verses", "General Wisdom", 3, 1, "वह परिवर्तन बनें जो आप दुनिया में देखना चाहते हैं।"),
    ("wisdom", "bn", "Inspirational Verses", "General Wisdom", 3, 1, "তুমি নিজেই সেই পরিবর্তন হও যা তুমি পৃথিবীতে দেখতে চাও।"),
]

# tradition → [language codes in display order] (first 3 shown in UI)
RELIGION_LANGUAGES = {
    "hinduism":      ["hi", "en", "bn"],
    "christianity":  ["en", "hi", "es"],
    "islam":         ["ar", "en", "ur"],
    "buddhism":      ["en", "hi", "th"],
    "general wisdom": ["en", "hi", "bn"],
}


def seed_database(db: Session) -> None:
    religions_data = [
        ("Hinduism", "hinduism"),
        ("Christianity", "christianity"),
        ("Islam", "islam"),
        ("Buddhism", "buddhism"),
        ("General Wisdom", "general wisdom"),
    ]
    languages_data = [
        ("English", "en", "English"),
        ("Hindi", "hi", "हिन्दी"),
        ("Bengali", "bn", "বাংলা"),
        ("Spanish", "es", "Español"),
        ("Arabic", "ar", "عربي"),
        ("Urdu", "ur", "اردو"),
        ("Thai", "th", "ภาษาไทย"),
    ]

    for name, code in religions_data:
        if not db.scalar(select(Religion).where(Religion.code == code)):
            db.add(Religion(name=name, code=code))
    for name, code, native_script in languages_data:
        existing = db.scalar(select(Language).where(Language.code == code))
        if existing is None:
            db.add(Language(name=name, code=code, native_script=native_script))
        elif existing.native_script == "":
            existing.native_script = native_script
    db.flush()

    religion_map = {r.code: r for r in db.scalars(select(Religion)).all()}
    language_map = {l.code: l for l in db.scalars(select(Language)).all()}

    # Seed religion-language mappings
    for religion_code, lang_codes in RELIGION_LANGUAGES.items():
        religion = religion_map.get(religion_code)
        if religion is None:
            continue
        for order, lang_code in enumerate(lang_codes):
            lang = language_map.get(lang_code)
            if lang is None:
                continue
            existing = db.scalar(
                select(ReligionLanguage).where(
                    ReligionLanguage.religion_id == religion.id,
                    ReligionLanguage.language_id == lang.id,
                )
            )
            if existing is None:
                db.add(ReligionLanguage(religion_id=religion.id, language_id=lang.id, sort_order=order))
    db.flush()

    # Seed scriptures
    scriptures = {}
    for code, _, name, religion_name, *_ in CONTENT:
        if code not in scriptures:
            row = db.scalar(select(Scripture).where(Scripture.code == code))
            if row is None:
                row = Scripture(name=name, code=code, religion_id=religion_map[religion_name.lower()].id)
                db.add(row)
                db.flush()
            scriptures[code] = row

    # Seed verses
    for scripture_code, language_code, _, _, chapter, number, text in CONTENT:
        lang = language_map.get(language_code)
        if lang is None:
            continue
        existing = db.scalar(
            select(Verse).where(
                Verse.scripture_id == scriptures[scripture_code].id,
                Verse.language_id == lang.id,
                Verse.chapter == chapter,
                Verse.verse_number == number,
            )
        )
        if existing is None:
            db.add(Verse(
                scripture_id=scriptures[scripture_code].id,
                language_id=lang.id,
                book=None, chapter=chapter, verse_number=number, text=text,
                translation="Original illustrative paraphrase — not a canonical translation.",
                source="Scripture Daily demo content",
                license="Original demo paraphrase; replace with licensed content",
                is_demo=True,
            ))
    db.commit()
