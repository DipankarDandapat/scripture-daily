"""Dhammapada — 70 more verses (v2) × en / hi / th, bringing Buddhism to 100 verses per language.

WisdomOne's plain-language renderings of the Pali original (public domain), using the standard verse numbering.
Hindi and Thai renderings should be proof-read by native speakers before a public release. Row format as in dhammapada.py.
"""

VERSES = [
    ("Yamakavagga", 1, 14, "self-control,wisdom", "anxious,angry", {
        "en": ("As rain does not break through a well-thatched house, so passion does not break through a well-trained mind.", "A trained, steady mind keeps strong feelings from flooding in."),
        "hi": ("जैसे अच्छी तरह छाई हुई छत में वर्षा नहीं घुसती, वैसे ही भली-भाँति साधे हुए चित्त में राग नहीं घुसता।", "सधा हुआ, स्थिर मन तेज़ भावनाओं को भीतर बाढ़ की तरह नहीं आने देता।"),
        "th": ("เหมือนฝนไม่อาจรั่วรดเรือนที่มุงดีแล้ว ราคะก็ไม่อาจรั่วรดจิตที่อบรมดีแล้วฉันนั้น", "จิตที่ฝึกดีแล้วและมั่นคง ย่อมไม่ปล่อยให้อารมณ์แรงท่วมท้น")}),
    ("Yamakavagga", 1, 16, "gratitude,wisdom", "happy,sad", {
        "en": ("The doer of good rejoices in this world and the next; they rejoice and are glad, seeing the purity of their own actions.", "Doing good brings a quiet, lasting joy."),
        "hi": ("शुभ कर्म करने वाला यहाँ भी प्रसन्न होता है और आगे भी; अपने कर्मों की शुद्धता देखकर वह आनन्दित होता है।", "भला करने से एक शांत और स्थायी ख़ुशी मिलती है।"),
        "th": ("ผู้ทำกรรมดีย่อมเบิกบานในโลกนี้และโลกหน้า เขาเห็นความบริสุทธิ์แห่งกรรมของตนแล้วย่อมปลื้มใจ", "การทำดีนำความสุขที่เงียบสงบและยั่งยืนมาให้")}),
    ("Yamakavagga", 1, 18, "hope,gratitude", "happy", {
        "en": ("The doer of good delights here and hereafter; thinking 'I have done good,' they delight, and delight even more when going on in happiness.", "Good deeds keep giving happiness again and again."),
        "hi": ("शुभ करने वाला यहाँ भी आनन्दित होता है और आगे भी; 'मैंने भला किया' सोचकर वह प्रसन्न होता है।", "अच्छे कर्म बार-बार ख़ुशी देते रहते हैं।"),
        "th": ("ผู้ทำดีย่อมรื่นเริงทั้งในโลกนี้และโลกหน้า คิดว่า 'เราได้ทำความดีแล้ว' ก็ยิ่งปีติยินดี", "ความดีที่ทำไว้ให้ความสุขซ้ำแล้วซ้ำเล่า")}),
    ("Appamadavagga", 2, 21, "wisdom,self-control", "anxious,happy", {
        "en": ("Heedfulness is the path to the deathless; heedlessness is the path to death. The heedful do not die; the heedless are as if already dead.", "Paying attention to life keeps us truly alive."),
        "hi": ("अप्रमाद अमृत का मार्ग है और प्रमाद मृत्यु का मार्ग है। अप्रमादी नहीं मरते; प्रमादी तो मृतक समान हैं।", "जीवन पर ध्यान देना हमें सचमुच जीवित रखता है।"),
        "th": ("ความไม่ประมาทเป็นทางไปสู่อมตะ ความประมาทเป็นทางแห่งความตาย ผู้ไม่ประมาทย่อมไม่ตาย ผู้ประมาทเหมือนตายแล้ว", "การใส่ใจชีวิตทำให้เรามีชีวิตอย่างแท้จริง")}),
    ("Appamadavagga", 2, 26, "wisdom,humility", "happy,anxious", {
        "en": ("Fools of little wit give themselves over to heedlessness, but the wise guard heedfulness as their best treasure.", "The wise treasure their attention and care."),
        "hi": ("अज्ञानी मूर्ख प्रमाद में लगे रहते हैं, पर बुद्धिमान अप्रमाद को श्रेष्ठ धन की तरह सँभालकर रखते हैं।", "समझदार लोग अपने ध्यान और सजगता को सबसे क़ीमती धन मानते हैं।"),
        "th": ("คนพาลมีปัญญาทรามย่อมหมกมุ่นในความประมาท ส่วนผู้มีปัญญารักษาความไม่ประมาทไว้ดุจทรัพย์อันประเสริฐ", "ผู้รู้ย่อมหวงแหนความใส่ใจและความระมัดระวังของตน")}),
    ("Cittavagga", 3, 33, "self-control,peace", "anxious,angry", {
        "en": ("Just as a fletcher straightens an arrow, the wise person steadies the flickering, fickle mind that is hard to guard and hard to control.", "With patient practice, even a restless mind can be steadied."),
        "hi": ("जैसे बाण बनाने वाला तीर को सीधा करता है, वैसे ही बुद्धिमान चंचल, अस्थिर और कठिनाई से सँभलने वाले चित्त को सीधा करता है।", "धैर्य भरे अभ्यास से बेचैन मन भी स्थिर हो सकता है।"),
        "th": ("ช่างศรย่อมดัดลูกศรให้ตรงฉันใด ผู้มีปัญญาย่อมทำจิตที่ดิ้นรน กวัดแกว่ง รักษายาก ห้ามยาก ให้ตรงฉันนั้น", "ด้วยการฝึกอย่างอดทน แม้จิตที่กระสับกระส่ายก็สงบได้")}),
    ("Cittavagga", 3, 36, "peace,wisdom", "anxious", {
        "en": ("The mind is hard to see, subtle and swift, and lands wherever it pleases. A wise person guards it, for a guarded mind brings happiness.", "Watching our thoughts gently brings real happiness."),
        "hi": ("चित्त कठिनाई से दिखाई देता है, सूक्ष्म और तेज़ है, जहाँ चाहे चला जाता है। बुद्धिमान उसकी रक्षा करे, क्योंकि रक्षित चित्त सुख लाता है।", "अपने विचारों को कोमलता से देखते रहना सच्ची ख़ुशी देता है।"),
        "th": ("จิตเห็นได้ยาก ละเอียดอ่อน เร็ว ไปตามที่ปรารถนา ผู้มีปัญญาพึงรักษาจิต เพราะจิตที่รักษาดีแล้วนำสุขมาให้", "การเฝ้าดูความคิดอย่างอ่อนโยนนำความสุขที่แท้จริงมาให้")}),
    ("Cittavagga", 3, 37, "peace,self-control", "anxious", {
        "en": ("Far-travelling, solitary, bodiless, lying in the cave of the heart: those who subdue the mind are freed from the bonds of Mara.", "Taming the mind frees us from what ties us down."),
        "hi": ("दूर तक जाने वाला, अकेला, अशरीरी, हृदय की गुफ़ा में रहने वाला चित्त जो वश में कर लेते हैं, वे मार के बंधन से मुक्त हो जाते हैं।", "मन को साध लेने से हम उन चीज़ों से छूट जाते हैं जो हमें बाँधती हैं।"),
        "th": ("จิตเที่ยวไปไกล ดวงเดียว ไม่มีรูปร่าง อาศัยถ้ำคือหัวใจ ผู้ใดสำรวมจิตได้ ย่อมพ้นจากบ่วงมาร", "การฝึกจิตให้สงบทำให้เราหลุดพ้นจากสิ่งที่ผูกมัด")}),
    ("Cittavagga", 3, 39, "peace,courage", "anxious", {
        "en": ("For one who is awake, whose mind is not soaked by craving or hatred, there is no fear.", "A clear, calm mind has nothing to be afraid of."),
        "hi": ("जो जागरूक है और जिसका चित्त राग-द्वेष से भीगा नहीं है, उसके लिए भय नहीं है।", "साफ़ और शांत मन के लिए डरने की कोई बात नहीं।"),
        "th": ("ผู้ตื่นอยู่ ผู้มีจิตไม่ชุ่มด้วยราคะและโทสะ ย่อมไม่มีภัย", "จิตที่ใสและสงบไม่มีสิ่งใดต้องกลัว")}),
    ("Cittavagga", 3, 40, "courage,self-control", "anxious,angry", {
        "en": ("Knowing this body is like a clay pot, make the mind firm like a fortress and fight Mara with the sword of wisdom.", "Know that the body is fragile and build inner strength with wisdom."),
        "hi": ("इस शरीर को मिट्टी के घड़े जैसा जानकर, चित्त को क़िले की तरह दृढ़ बनाओ और प्रज्ञा के शस्त्र से मार से लड़ो।", "शरीर नाज़ुक है यह जानकर समझ से भीतरी ताक़त बनाओ।"),
        "th": ("รู้ว่ากายนี้เปราะบางดุจหม้อดิน จงตั้งจิตให้มั่นดุจเมือง แล้วรบมารด้วยอาวุธคือปัญญา", "รู้ว่าร่างกายเปราะบาง จงสร้างความเข้มแข็งภายในด้วยปัญญา")}),
    ("Cittavagga", 3, 42, "wisdom,self-control", "angry,sad", {
        "en": ("Whatever harm an enemy may do to an enemy, a wrongly directed mind can do to us even worse.", "Our own wrong thoughts can hurt us more than anyone else."),
        "hi": ("शत्रु शत्रु का जितना अहित करता है, उससे भी अधिक अहित ग़लत दिशा में गया हुआ चित्त हमारा करता है।", "हमारे अपने ग़लत विचार हमें किसी और से ज़्यादा चोट पहुँचा सकते हैं।"),
        "th": ("โจรทำร้ายโจร หรือเวรทำร้ายผู้จองเวรได้เท่าใด จิตที่ตั้งไว้ผิดย่อมทำร้ายเราได้ยิ่งกว่านั้น", "ความคิดผิด ๆ ของเราเองอาจทำร้ายเรามากกว่าใคร")}),
    ("Pupphavagga", 4, 52, "truth,wisdom", "happy", {
        "en": ("Like a beautiful flower, colourful and fragrant, are the fair words of one who acts accordingly.", "Kind words are most beautiful when matched by kind actions."),
        "hi": ("जैसे सुन्दर रंग और सुगंध वाला फूल, वैसे ही सुन्दर वचन उसके हैं जो उसके अनुसार आचरण भी करता है।", "दयालु शब्द तब सबसे सुंदर होते हैं जब कर्म भी दयालु हों।"),
        "th": ("ดอกไม้งามมีทั้งสีและกลิ่นหอมฉันใด วาจาสุภาษิตของผู้ประพฤติตามย่อมมีผลฉันนั้น", "ถ้อยคำดีงามที่สุดเมื่อการกระทำสอดคล้องกัน")}),
    ("Pupphavagga", 4, 53, "charity,wisdom", "happy", {
        "en": ("As from a heap of flowers many garlands can be made, so many good deeds should be done by one born as a mortal.", "Every day gives us a chance to make many small good things."),
        "hi": ("जैसे फूलों के ढेर से अनेक मालाएँ बन सकती हैं, वैसे ही मनुष्य जन्म पाकर अनेक शुभ कर्म करने चाहिए।", "हर दिन हमें कई छोटी-छोटी अच्छी चीज़ें करने का मौक़ा देता है।"),
        "th": ("พวงมาลัยมากมายร้อยได้จากกองดอกไม้ฉันใด ผู้เกิดเป็นมนุษย์พึงทำกรรมดีให้มากฉันนั้น", "ทุกวันให้โอกาสเราทำสิ่งดีเล็ก ๆ มากมาย")}),
    ("Pupphavagga", 4, 55, "wisdom,humility", "happy", {
        "en": ("Of sandalwood, tagara, lotus and jasmine, the fragrance of virtue surpasses them all.", "Good character is a fragrance that is nicer than any perfume."),
        "hi": ("चन्दन, तगर, कमल और चमेली की सुगंध से भी शील की सुगंध श्रेष्ठ है।", "अच्छा चरित्र किसी भी इत्र से ज़्यादा मनमोहक सुगंध है।"),
        "th": ("กลิ่นจันทน์ กฤษณา ดอกบัว และมะลิ ก็ไม่ประเสริฐเท่ากลิ่นแห่งศีล", "ศีลธรรมเป็นกลิ่นหอมที่ดีกว่าน้ำหอมใด ๆ")}),
    ("Balavagga", 5, 60, "patience,wisdom", "sad,anxious", {
        "en": ("Long is the night to the sleepless; long is the road to the weary; long is the cycle of life to fools who do not know the true teaching.", "Without understanding, things feel endless; with understanding, they lighten."),
        "hi": ("जागने वाले के लिए रात लंबी है; थके हुए के लिए योजन लंबा है; सद्धर्म न जानने वाले मूर्खों के लिए संसार का चक्र लंबा है।", "समझ के बिना सब कुछ अंतहीन लगता है; समझ आने पर हल्का हो जाता है।"),
        "th": ("ราตรียาวนานสำหรับผู้ไม่หลับ ทางยาวไกลสำหรับผู้เหนื่อยล้า สังสารวัฏยาวนานสำหรับคนพาลผู้ไม่รู้พระสัทธรรม", "เมื่อขาดความเข้าใจทุกอย่างดูไม่มีที่สิ้นสุด เมื่อเข้าใจก็เบาลง")}),
    ("Balavagga", 5, 63, "humility,wisdom", "happy,angry", {
        "en": ("The fool who knows he is a fool is wise to that extent; but the fool who thinks himself wise is called a fool indeed.", "Knowing what we do not know is the start of learning."),
        "hi": ("जो मूर्ख अपनी मूर्खता जानता है, वह उतने में बुद्धिमान है; पर जो मूर्ख स्वयं को बुद्धिमान समझता है, वही सचमुच मूर्ख कहलाता है।", "जो हम नहीं जानते उसे पहचानना सीखने की शुरुआत है।"),
        "th": ("คนพาลที่รู้ว่าตนพาลก็นับว่าเป็นปราชญ์ในข้อนั้น แต่คนพาลที่สำคัญว่าตนเป็นบัณฑิต แท้จริงเรียกว่าพาล", "การรู้ว่าตนยังไม่รู้อะไรคือจุดเริ่มต้นของการเรียนรู้")}),
    ("Panditavagga", 6, 77, "wisdom,compassion", "happy,angry", {
        "en": ("Let one advise, instruct and guard against wrongdoing; such a person is dear to the good and disliked by the bad.", "Gently guiding others away from harm is a true kindness."),
        "hi": ("जो समझाए, सिखाए और अनुचित से रोके, वह सज्जनों को प्रिय और असज्जनों को अप्रिय होता है।", "दूसरों को कोमलता से हानि से बचाना सच्ची दया है।"),
        "th": ("ผู้ใดพึงตักเตือน พร่ำสอน ห้ามปรามจากสิ่งไม่ควร ผู้นั้นเป็นที่รักของสัตบุรุษ และไม่เป็นที่รักของอสัตบุรุษ", "การช่วยแนะนำผู้อื่นให้พ้นจากสิ่งเสียหายอย่างอ่อนโยนคือความเมตตาแท้")}),
    ("Panditavagga", 6, 78, "wisdom,love", "happy", {
        "en": ("Do not keep company with bad friends; do not keep company with the lowest of people. Keep company with good friends, with the best of people.", "The friends we keep shape the people we become."),
        "hi": ("बुरे मित्रों की संगति मत करो, नीच पुरुषों की संगति मत करो; अच्छे मित्रों की संगति करो, उत्तम पुरुषों की संगति करो।", "हम जिन मित्रों के साथ रहते हैं वे हमें बनाते हैं।"),
        "th": ("อย่าคบมิตรชั่ว อย่าคบคนต่ำช้า จงคบกัลยาณมิตร จงคบบุรุษผู้สูงสุด", "เพื่อนที่เราคบหล่อหลอมให้เราเป็นคนอย่างไร")}),
    ("Panditavagga", 6, 79, "peace,wisdom", "happy,anxious", {
        "en": ("One who drinks in the Dhamma lives happily with a serene mind; the wise always delight in the teaching taught by the noble.", "Learning good things calms the heart."),
        "hi": ("जो धर्म का पान करता है वह प्रसन्न चित्त से सुख से रहता है; पंडित सदा आर्यों द्वारा बताए धर्म में रमण करता है।", "अच्छी बातें सीखने से हृदय शांत होता है।"),
        "th": ("ผู้ดื่มธรรมย่อมอยู่เป็นสุขด้วยใจผ่องใส บัณฑิตย่อมยินดีในธรรมที่พระอริยะแสดงไว้เสมอ", "การเรียนรู้สิ่งดีทำให้ใจสงบ")}),
    ("Panditavagga", 6, 80, "self-control,wisdom", "happy,angry", {
        "en": ("Irrigators guide the water; fletchers shape the arrow; carpenters shape the wood; the wise tame themselves.", "The wisest work is shaping ourselves."),
        "hi": ("सिंचाई करने वाले जल को ले जाते हैं, बाण बनाने वाले तीर को सीधा करते हैं, बढ़ई लकड़ी को गढ़ते हैं; पंडित स्वयं को साधते हैं।", "सबसे समझदारी का काम अपने आपको गढ़ना है।"),
        "th": ("ชาวนาย่อมไขน้ำเข้านา ช่างศรย่อมดัดลูกศร ช่างไม้ย่อมถากไม้ บัณฑิตย่อมฝึกตน", "งานที่ฉลาดที่สุดคือการฝึกตนเอง")}),
    ("Panditavagga", 6, 85, "wisdom,courage", "anxious,happy", {
        "en": ("Few among people cross to the farther shore; the rest only run up and down along this bank.", "Real change takes the courage to actually cross over."),
        "hi": ("लोगों में थोड़े ही उस पार पहुँचते हैं; बाक़ी तो इसी किनारे पर इधर-उधर दौड़ते रहते हैं।", "सच्चे बदलाव के लिए सचमुच पार जाने की हिम्मत चाहिए।"),
        "th": ("ในหมู่มนุษย์ มีน้อยคนที่ข้ามไปถึงฝั่ง คนที่เหลือเพียงวิ่งเลาะอยู่ริมฝั่งนี้", "การเปลี่ยนแปลงที่แท้ต้องอาศัยความกล้าที่จะข้ามไปจริง ๆ")}),
    ("Arahantavagga", 7, 96, "peace,wisdom", "anxious,happy", {
        "en": ("Calm is their thought, calm their speech, calm their deed — one who is freed by right understanding and perfectly at peace.", "Peace shows in what we think, say and do."),
        "hi": ("शांत है उसका मन, शांत है उसकी वाणी, शांत है उसका कर्म; जो सम्यक् ज्ञान से मुक्त और पूर्ण शांत है।", "शांति हमारे सोचने, बोलने और करने में दिखाई देती है।"),
        "th": ("ใจของเขาสงบ วาจาก็สงบ การกระทำก็สงบ ผู้หลุดพ้นด้วยปัญญาชอบ เป็นผู้สงบระงับ", "ความสงบปรากฏในสิ่งที่เราคิด พูด และทำ")}),
    ("Sahassavagga", 8, 104, "self-control,courage", "angry,happy", {
        "en": ("Victory over oneself is better than the conquest of other people.", "The greatest win is mastering your own mind."),
        "hi": ("दूसरों को जीतने से अपने आप को जीतना श्रेष्ठ है।", "सबसे बड़ी जीत अपने मन पर क़ाबू पाना है।"),
        "th": ("การชนะตนเองประเสริฐกว่าการชนะผู้อื่น", "ชัยชนะที่ยิ่งใหญ่ที่สุดคือการเอาชนะใจตนเอง")}),
    ("Sahassavagga", 8, 109, "humility,gratitude", "happy", {
        "en": ("For one who constantly honours and respects the elders, four things increase: life, beauty, happiness and strength.", "Respecting elders brings good things into our lives."),
        "hi": ("जो नित्य बड़ों का आदर और सेवा करता है, उसके चार बातें बढ़ती हैं: आयु, सुन्दरता, सुख और बल।", "बड़ों का आदर करने से जीवन में अच्छी चीज़ें बढ़ती हैं।"),
        "th": ("ผู้มีปกติกราบไหว้และเคารพผู้ใหญ่เป็นนิตย์ ย่อมเจริญด้วยธรรมสี่ประการ คือ อายุ วรรณะ สุข และพละ", "การเคารพผู้ใหญ่นำสิ่งดี ๆ มาสู่ชีวิต")}),
    ("Sahassavagga", 8, 110, "wisdom,self-control", "happy,anxious", {
        "en": ("Better than a hundred years lived immoral and uncontrolled is one day lived virtuous and meditative.", "One day lived well is worth more than years lived carelessly."),
        "hi": ("शीलरहित और असंयमी रहकर सौ वर्ष जीने से शीलवान और ध्यानी रहकर एक दिन जीना श्रेष्ठ है।", "अच्छी तरह जिया गया एक दिन लापरवाही से जिए गए बरसों से अधिक मूल्यवान है।"),
        "th": ("ผู้ทุศีลไม่สำรวม มีชีวิตอยู่ร้อยปี สู้ผู้มีศีลมีฌาน มีชีวิตอยู่วันเดียวไม่ได้", "หนึ่งวันที่ใช้ชีวิตดีมีค่ากว่าหลายปีที่ใช้อย่างประมาท")}),
    ("Sahassavagga", 8, 111, "wisdom,peace", "happy,anxious", {
        "en": ("Better than a hundred years lived without wisdom or calm is one day lived wise and meditative.", "Wisdom and calm make even one day rich."),
        "hi": ("प्रज्ञा और ध्यान के बिना सौ वर्ष जीने से प्रज्ञावान और ध्यानी रहकर एक दिन जीना श्रेष्ठ है।", "प्रज्ञा और शांति एक दिन को भी समृद्ध बना देती हैं।"),
        "th": ("ผู้ไร้ปัญญาไม่มีฌาน มีชีวิตอยู่ร้อยปี สู้ผู้มีปัญญามีฌาน มีชีวิตอยู่วันเดียวไม่ได้", "ปัญญาและความสงบทำให้แม้เพียงหนึ่งวันก็มีค่า")}),
    ("Papavagga", 9, 116, "courage,self-control", "anxious,happy", {
        "en": ("Hasten to do good; restrain the mind from evil. If you are slow in doing good, the mind finds pleasure in wrong.", "Do the good thing right away, before hesitation grows."),
        "hi": ("भलाई करने में शीघ्रता करो, पाप से चित्त को रोको; जो भलाई में देर करता है, उसका मन पाप में रमने लगता है।", "अच्छा काम तुरंत कर डालो, झिझक बढ़ने से पहले।"),
        "th": ("พึงรีบทำความดี พึงห้ามจิตจากความชั่ว เมื่อทำความดีช้า ใจย่อมยินดีในความชั่ว", "ทำความดีทันที ก่อนที่ความลังเลจะมากขึ้น")}),
    ("Papavagga", 9, 118, "wisdom,gratitude", "happy", {
        "en": ("If a person does good, let them do it again and again; let them take delight in it, for happiness is the outcome of good.", "Good deeds repeated become a happy habit."),
        "hi": ("यदि कोई शुभ कर्म करे तो उसे बार-बार करे; उसमें रमे, क्योंकि शुभ का संचय सुखदायी है।", "बार-बार किए गए अच्छे कर्म ख़ुशी की आदत बन जाते हैं।"),
        "th": ("หากบุคคลทำความดี พึงทำบ่อย ๆ พึงพอใจในความดีนั้น เพราะการสั่งสมความดีนำสุขมาให้", "ความดีที่ทำซ้ำ ๆ จะกลายเป็นนิสัยแห่งความสุข")}),
    ("Dandavagga", 10, 133, "self-control,compassion", "angry", {
        "en": ("Do not speak harshly to anyone; those spoken to will answer you in the same way. Angry speech is painful, and blows may return to you.", "Harsh words come back to us, so speak gently."),
        "hi": ("किसी से कठोर वचन मत बोलो; जिनसे बोलोगे वे भी वैसा ही उत्तर देंगे। क्रोध भरी बात दुखदायी होती है, प्रतिघात तुम्हीं तक लौट आता है।", "कठोर शब्द हमारे पास लौट आते हैं, इसलिए कोमलता से बोलो।"),
        "th": ("อย่าพูดคำหยาบกับใคร ผู้ที่ถูกพูดด้วยจะโต้ตอบเจ้า คำพูดเกรี้ยวกราดเป็นทุกข์ การโต้ตอบจะกลับมาถึงเจ้า", "คำพูดรุนแรงย้อนกลับมาหาเรา จงพูดด้วยความอ่อนโยน")}),
    ("Jaravagga", 11, 151, "wisdom,hope", "sad,happy", {
        "en": ("Even royal chariots wear out and the body grows old, but the teaching of the good never grows old; the good share it with the good.", "Good teachings stay fresh forever, even as bodies age."),
        "hi": ("राजाओं के सुंदर रथ भी जर्जर हो जाते हैं, शरीर भी बूढ़ा होता है; पर सज्जनों का धर्म बूढ़ा नहीं होता; सज्जन सज्जनों से कहते हैं।", "शरीर बूढ़े होते हैं, पर अच्छी सीख हमेशा ताज़ा रहती है।"),
        "th": ("แม้รถหลวงอันวิจิตรย่อมคร่ำคร่า แม้สรีระก็ถึงความชรา แต่ธรรมของสัตบุรุษไม่ชรา สัตบุรุษย่อมบอกกันและกัน", "ร่างกายชรา แต่คำสอนที่ดียังสดใหม่เสมอ")}),
    ("Attavagga", 12, 158, "wisdom,truth", "happy,angry", {
        "en": ("Let one first establish oneself in what is proper; then teach others. Thus the wise will not be blamed.", "Live by your own advice before giving it."),
        "hi": ("पहले स्वयं को उचित में स्थापित करे, फिर दूसरों को सिखाए; ऐसा पंडित निन्दा का पात्र नहीं होता।", "सलाह देने से पहले उसे ख़ुद अपनाओ।"),
        "th": ("พึงตั้งตนไว้ในสิ่งที่สมควรก่อน แล้วจึงสอนผู้อื่น บัณฑิตเช่นนั้นย่อมไม่มัวหมอง", "จงทำตามคำแนะนำของตนเองก่อนที่จะแนะนำผู้อื่น")}),
    ("Attavagga", 12, 159, "self-control,wisdom", "angry,anxious", {
        "en": ("One should do what one teaches others to do. Self-control is difficult, indeed.", "Taming ourselves is the hardest and most important lesson."),
        "hi": ("जैसा दूसरों को सिखाए, वैसा स्वयं करे; स्वयं को साधना निश्चय ही कठिन है।", "अपने आपको साधना सबसे कठिन और सबसे ज़रूरी पाठ है।"),
        "th": ("สอนผู้อื่นอย่างใด พึงทำตนอย่างนั้น ผู้ฝึกตนดีแล้วจึงฝึกผู้อื่นได้ ตนนั้นแลฝึกได้ยาก", "การฝึกตนเองเป็นบทเรียนที่ยากและสำคัญที่สุด")}),
    ("Attavagga", 12, 163, "wisdom,courage", "sad,anxious", {
        "en": ("Easy to do are things that are bad and harmful to oneself; very hard to do are things good and beneficial.", "Good choices often take effort, and that is what makes them worth it."),
        "hi": ("अपने लिए बुरे और हानिकारक कर्म करना सरल है; जो हितकर और भले हैं, उन्हें करना अत्यंत कठिन है।", "अच्छे निर्णयों में अक्सर मेहनत लगती है, और यही उन्हें मूल्यवान बनाती है।"),
        "th": ("ความชั่วและสิ่งไม่เป็นประโยชน์แก่ตนทำได้ง่าย ส่วนสิ่งดีและเป็นประโยชน์ทำได้ยากยิ่ง", "การตัดสินใจที่ดีมักต้องใช้ความพยายาม และนั่นคือสิ่งที่ทำให้มีค่า")}),
    ("Attavagga", 12, 166, "wisdom,truth", "happy,anxious", {
        "en": ("Do not neglect your own true welfare for another's, however great; knowing your own good, be intent on the goal.", "Caring for your own path is not selfish; it helps you help others."),
        "hi": ("दूसरे के हित के लिए, चाहे वह कितना ही बड़ा हो, अपने सच्चे हित की उपेक्षा न करे; अपना हित जानकर लक्ष्य में लगा रहे।", "अपने मार्ग का ध्यान रखना स्वार्थ नहीं, यह दूसरों की मदद करने में सहायक है।"),
        "th": ("อย่าละทิ้งประโยชน์ตนเพราะประโยชน์ผู้อื่นแม้ใหญ่หลวง รู้ประโยชน์ตนแล้วพึงขวนขวายในประโยชน์นั้น", "การใส่ใจเส้นทางของตนไม่ใช่ความเห็นแก่ตัว แต่ช่วยให้ช่วยผู้อื่นได้")}),
    ("Lokavagga", 13, 168, "wisdom,self-control", "happy,anxious", {
        "en": ("Rise up and do not be heedless; follow the path of good conduct. One who lives by it lives happily in this world and the next.", "Wake up to what matters, and live by good conduct."),
        "hi": ("उठो, प्रमाद मत करो, सदाचार के धर्म का पालन करो; जो धर्म पर चलता है वह इस लोक और परलोक में सुख से रहता है।", "जो ज़रूरी है उसके प्रति जागो और अच्छे आचरण से जियो।"),
        "th": ("จงลุกขึ้น อย่าประมาท จงประพฤติธรรมให้สุจริต ผู้ประพฤติธรรมย่อมอยู่เป็นสุขทั้งในโลกนี้และโลกหน้า", "ตื่นขึ้นสู่สิ่งสำคัญ และใช้ชีวิตด้วยความประพฤติที่ดี")}),
    ("Lokavagga", 13, 170, "wisdom,peace", "anxious,sad", {
        "en": ("Look upon the world as a bubble, look upon it as a mirage; the king of death does not see the one who looks upon the world in this way.", "Seeing how fleeting things are can loosen worry."),
        "hi": ("संसार को बुलबुले की तरह देखो, मृगतृष्णा की तरह देखो; जो इस तरह देखता है उसे मृत्यु का राजा नहीं देख पाता।", "चीज़ों की क्षणभंगुरता देखने से चिंता ढीली पड़ती है।"),
        "th": ("จงมองโลกนี้ดุจฟองน้ำ จงมองโลกนี้ดุจพยับแดด พญามัจจุราชย่อมไม่เห็นผู้มองโลกอย่างนี้", "การเห็นความไม่เที่ยงของสิ่งต่าง ๆ ช่วยคลายความกังวล")}),
    ("Lokavagga", 13, 172, "hope,wisdom", "sad,happy", {
        "en": ("One who was heedless before and later is not, brightens this world like the moon freed from a cloud.", "It is never too late to wake up and begin again."),
        "hi": ("जो पहले प्रमादी था और बाद में प्रमाद नहीं करता, वह बादल से मुक्त चन्द्रमा की तरह इस लोक को प्रकाशित करता है।", "जागने और फिर से शुरू करने में कभी देर नहीं होती।"),
        "th": ("ผู้ที่เคยประมาทภายหลังไม่ประมาท ย่อมทำโลกนี้ให้สว่างดุจพระจันทร์พ้นจากเมฆ", "ไม่มีวันสายเกินไปที่จะตื่นและเริ่มต้นใหม่")}),
    ("Buddhavagga", 14, 182, "gratitude,wisdom", "happy", {
        "en": ("Hard is it to gain birth as a human being; hard is the life of mortals; hard to hear the true teaching; hard to meet an awakened one.", "Being alive and able to learn is a rare gift."),
        "hi": ("मनुष्य जन्म पाना दुर्लभ है; मरणधर्मा जीवन दुर्लभ है; सद्धर्म का सुनना दुर्लभ है; बुद्धों का उत्पन्न होना दुर्लभ है।", "जीवित होना और सीख पाना एक दुर्लभ उपहार है।"),
        "th": ("การได้เกิดเป็นมนุษย์ยาก การมีชีวิตอยู่ยาก การได้ฟังพระสัทธรรมยาก การเกิดขึ้นแห่งพระพุทธเจ้ายาก", "การมีชีวิตและมีโอกาสเรียนรู้เป็นของขวัญที่หาได้ยาก")}),
    ("Buddhavagga", 14, 185, "self-control,peace", "angry,anxious", {
        "en": ("Not insulting, not harming, restraint, moderation in food, a quiet dwelling and devotion to higher thought: this is the teaching of the awakened.", "A simple life of kindness and calm is the heart of the path."),
        "hi": ("निन्दा न करना, हानि न पहुँचाना, संयम, भोजन में मात्रा, एकांत निवास और उच्च चिंतन में लगन — यही बुद्धों का उपदेश है।", "दया और शांति से भरा सादा जीवन ही मार्ग का हृदय है।"),
        "th": ("การไม่ว่าร้าย ไม่ทำร้าย ความสำรวม ความรู้จักประมาณในอาหาร ที่นั่งนอนอันสงัด และความเพียรในจิตอันยิ่ง นี้คือคำสอนของพระพุทธเจ้าทั้งหลาย", "ชีวิตที่เรียบง่ายด้วยเมตตาและความสงบคือหัวใจของทางนี้")}),
    ("Buddhavagga", 14, 194, "peace,love", "happy", {
        "en": ("Blessed is the birth of awakened ones; blessed is the teaching of the true Dhamma; blessed is the harmony of the community; blessed is the effort of those who live in harmony.", "Living in harmony together is a great blessing."),
        "hi": ("बुद्धों का उत्पन्न होना सुखदायी है; सद्धर्म का उपदेश सुखदायी है; संघ की एकता सुखदायी है; एकता वालों का तप सुखदायी है।", "मिल-जुलकर सद्भाव से रहना बड़ा आशीर्वाद है।"),
        "th": ("การเกิดขึ้นแห่งพระพุทธเจ้าเป็นสุข การแสดงพระสัทธรรมเป็นสุข ความสามัคคีของหมู่เป็นสุข ความเพียรของผู้พร้อมเพรียงเป็นสุข", "การอยู่ร่วมกันด้วยความสามัคคีเป็นพรอันยิ่งใหญ่")}),
    ("Sukhavagga", 15, 200, "peace,gratitude", "happy", {
        "en": ("Happily indeed we live, we who have nothing of our own; we shall feed on joy like the radiant gods.", "Joy comes from a light, free heart, not from owning many things."),
        "hi": ("अहो, हम सुखपूर्वक जीते हैं, जिनके पास अपना कुछ नहीं; हम प्रीति का भोजन करने वाले देवताओं की तरह होंगे।", "ख़ुशी हल्के, मुक्त हृदय से आती है, बहुत कुछ पास होने से नहीं।"),
        "th": ("เราผู้ไม่มีกิเลสเครื่องกังวล ย่อมอยู่เป็นสุขจริงหนอ เราจักเป็นผู้มีปีติเป็นภักษา ดุจเทวดาชั้นอาภัสสร", "ความสุขมาจากใจที่เบาและเป็นอิสระ ไม่ใช่จากการมีสิ่งของมากมาย")}),
    ("Sukhavagga", 15, 201, "peace,forgiveness", "angry,sad", {
        "en": ("Victory breeds hatred; the defeated live in pain. The peaceful live happily, having given up both victory and defeat.", "Letting go of winning and losing brings peace."),
        "hi": ("विजय वैर को जन्म देती है; पराजित दुख में रहता है। शांत पुरुष जय-पराजय दोनों छोड़कर सुख से रहता है।", "जीतने और हारने की चिंता छोड़ने से शांति मिलती है।"),
        "th": ("ชัยชนะก่อเวร ผู้แพ้อยู่เป็นทุกข์ ผู้สงบละทั้งชัยชนะและความพ่ายแพ้ได้ ย่อมอยู่เป็นสุข", "การปล่อยวางเรื่องแพ้ชนะนำความสงบมาให้")}),
    ("Sukhavagga", 15, 202, "peace,self-control", "angry,anxious", {
        "en": ("There is no fire like lust, no evil like hatred, no sorrow like clinging, and no happiness higher than peace.", "Peace is the best happiness there is."),
        "hi": ("राग के समान आग नहीं, द्वेष के समान पाप नहीं, स्कंधों के समान दुख नहीं, और शांति से बड़ा सुख नहीं।", "शांति सबसे श्रेष्ठ सुख है।"),
        "th": ("ไฟเสมอด้วยราคะไม่มี โทษเสมอด้วยโทสะไม่มี ทุกข์เสมอด้วยขันธ์ไม่มี สุขอื่นยิ่งกว่าความสงบไม่มี", "ความสงบคือความสุขที่ดีที่สุด")}),
    ("Sukhavagga", 15, 205, "peace,wisdom", "anxious,sad", {
        "en": ("Having tasted the sweetness of solitude and the sweetness of peace, one becomes free from fear and wrongdoing, drinking the joy of the Dhamma.", "Quiet time alone can bring deep, fearless joy."),
        "hi": ("एकांत का रस और शांति का रस चखकर मनुष्य भय और पाप से मुक्त हो जाता है और धर्म का आनन्द पीता है।", "अकेले बिताया शांत समय गहरी और निर्भय ख़ुशी दे सकता है।"),
        "th": ("ผู้ได้ลิ้มรสแห่งความสงัดและรสแห่งความสงบ ย่อมเป็นผู้ไม่มีความกลัวและไม่มีบาป ดื่มรสแห่งปีติในธรรม", "เวลาสงบอยู่ตามลำพังนำความสุขอันลึกซึ้งและไร้ความกลัวมาให้")}),
    ("Sukhavagga", 15, 206, "love,gratitude", "happy", {
        "en": ("It is good to see the noble ones; to live with them is always happiness.", "Spending time with good people lifts the heart."),
        "hi": ("आर्यों का दर्शन अच्छा है; उनके साथ रहना सदा सुखदायी है।", "अच्छे लोगों के साथ समय बिताना हृदय को ऊपर उठाता है।"),
        "th": ("การได้เห็นพระอริยะเป็นความดี การอยู่ร่วมกับท่านเป็นสุขทุกเมื่อ", "การใช้เวลากับคนดีทำให้ใจเบิกบาน")}),
    ("Piyavagga", 16, 212, "peace,wisdom", "sad,anxious", {
        "en": ("From what we hold dear springs grief, from what we hold dear springs fear; for one wholly free from clinging there is no grief, so how fear?", "Holding things less tightly means less worry."),
        "hi": ("प्रिय से शोक उत्पन्न होता है, प्रिय से भय उत्पन्न होता है; जो प्रिय से पूर्णतः मुक्त है उसे शोक नहीं, फिर भय कैसा?", "चीज़ों को कम कसकर पकड़ने से चिंता भी कम होती है।"),
        "th": ("ความโศกเกิดจากสิ่งเป็นที่รัก ภัยเกิดจากสิ่งเป็นที่รัก ผู้พ้นจากสิ่งเป็นที่รักแล้ว ย่อมไม่มีโศก จักมีภัยแต่ที่ไหน", "การยึดสิ่งต่าง ๆ ให้หลวมลง ทำให้กังวลน้อยลง")}),
    ("Kodhavagga", 17, 224, "truth,charity", "happy,angry", {
        "en": ("Speak the truth, do not yield to anger, and give even if you have little; by these three you come close to the gods.", "Honesty, calm and generosity make life good."),
        "hi": ("सच बोलो, क्रोध न करो, थोड़ा हो तो भी दो; इन तीन बातों से मनुष्य देवों के पास पहुँचता है।", "सच्चाई, शांति और उदारता जीवन को अच्छा बनाती हैं।"),
        "th": ("พึงพูดจริง ไม่พึงโกรธ แม้มีน้อยก็พึงให้ ด้วยเหตุสามประการนี้ย่อมไปใกล้เทวดาได้", "ความซื่อตรง ความสงบ และความเอื้อเฟื้อทำให้ชีวิตงดงาม")}),
    ("Kodhavagga", 17, 231, "self-control,peace", "angry", {
        "en": ("Guard against anger in the body; be restrained in body. Giving up bodily misconduct, practise good conduct with the body.", "Keep your actions gentle, especially when you are upset."),
        "hi": ("शरीर के क्रोध से बचो, शरीर में संयम रखो; शारीरिक दुराचार छोड़कर शरीर से सदाचार करो।", "ख़ासकर जब परेशान हों, अपने कर्म कोमल रखो।"),
        "th": ("พึงระวังความโกรธทางกาย พึงสำรวมกาย ละกายทุจริตแล้วประพฤติกายสุจริต", "จงรักษาการกระทำให้อ่อนโยนโดยเฉพาะเมื่อไม่พอใจ")}),
    ("Kodhavagga", 17, 232, "self-control,truth", "angry", {
        "en": ("Guard against anger in speech; be restrained in speech. Giving up wrong speech, practise good conduct with speech.", "Pause before you speak when you are angry."),
        "hi": ("वाणी के क्रोध से बचो, वाणी में संयम रखो; वाणी के दुराचार छोड़कर वाणी से सदाचार करो।", "ग़ुस्से में बोलने से पहले रुक जाओ।"),
        "th": ("พึงระวังความโกรธทางวาจา พึงสำรวมวาจา ละวจีทุจริตแล้วประพฤติวจีสุจริต", "เมื่อโกรธ จงหยุดก่อนพูด")}),
    ("Malavagga", 18, 239, "patience,self-control", "anxious,sad", {
        "en": ("Gradually, little by little, moment by moment, a wise person should remove their own impurities, as a smith removes dross from silver.", "Self-improvement is a slow, steady polishing."),
        "hi": ("क्रमशः, थोड़ा-थोड़ा करके, क्षण-क्षण, बुद्धिमान अपने मैल को वैसे दूर करे जैसे सुनार चाँदी का मैल दूर करता है।", "ख़ुद को बेहतर बनाना धीरे-धीरे और लगातार चमकाने जैसा है।"),
        "th": ("ผู้มีปัญญาพึงกำจัดมลทินของตนทีละน้อย ทีละขณะ ตามลำดับ ดุจช่างทองกำจัดสนิมเงิน", "การพัฒนาตนเองคือการขัดเกลาอย่างช้า ๆ และสม่ำเสมอ")}),
    ("Malavagga", 18, 252, "wisdom,humility", "angry,happy", {
        "en": ("The faults of others are easy to see, but our own are hard to see.", "Look at your own habits first, before judging others."),
        "hi": ("दूसरों के दोष देखना सरल है, पर अपने दोष देखना कठिन है।", "दूसरों को परखने से पहले अपनी आदतें देखो।"),
        "th": ("โทษของผู้อื่นเห็นได้ง่าย แต่โทษของตนเห็นได้ยาก", "จงมองนิสัยของตนก่อนตัดสินผู้อื่น")}),
    ("Maggavagga", 20, 273, "wisdom,peace", "anxious,happy", {
        "en": ("Of paths, the Eightfold is best; of truths, the Four Noble Truths; of states, freedom from craving; of people, the one who sees.", "A simple, balanced way of living is the best path."),
        "hi": ("मार्गों में अष्टांगिक मार्ग श्रेष्ठ है; सत्यों में चार आर्य सत्य; धर्मों में वैराग्य; मनुष्यों में देखने वाला।", "संतुलित और सादा जीवन-पथ सबसे अच्छा मार्ग है।"),
        "th": ("ในบรรดาทาง อัฏฐังคิกมรรคประเสริฐสุด ในบรรดาสัจจะ อริยสัจสี่ประเสริฐสุด ในบรรดาธรรม วิราคะประเสริฐสุด ในบรรดาสัตว์สองเท้า ผู้มีจักษุประเสริฐสุด", "วิถีชีวิตที่เรียบง่ายและสมดุลคือทางที่ดีที่สุด")}),
    ("Maggavagga", 20, 282, "wisdom,peace", "anxious,happy", {
        "en": ("From meditation wisdom springs; without meditation wisdom wanes. Knowing this twofold path of gain and loss, conduct yourself so wisdom grows.", "Quiet reflection helps wisdom grow."),
        "hi": ("ध्यान से प्रज्ञा उत्पन्न होती है, ध्यान के बिना प्रज्ञा क्षीण होती है; लाभ-हानि के इस दोहरे मार्ग को जानकर ऐसे चलो कि प्रज्ञा बढ़े।", "शांत चिंतन से समझ बढ़ती है।"),
        "th": ("ปัญญาเกิดจากการเจริญสมาธิ ปัญญาเสื่อมเพราะไม่เจริญสมาธิ รู้ทางแห่งความเจริญและเสื่อมนี้แล้ว พึงตั้งตนให้ปัญญางอกงาม", "การใคร่ครวญอย่างสงบช่วยให้ปัญญาเติบโต")}),
    ("Pakinnakavagga", 21, 290, "wisdom,courage", "happy,sad", {
        "en": ("If by giving up a small happiness one can find a greater happiness, the wise will give up the small for the great.", "Sometimes letting go of a little brings a lot more joy."),
        "hi": ("यदि छोटे सुख को छोड़ने से बड़ा सुख दिखाई दे, तो बुद्धिमान बड़े सुख के लिए छोटे सुख को छोड़ दे।", "कभी-कभी थोड़ा छोड़ देने से बहुत अधिक ख़ुशी मिलती है।"),
        "th": ("หากเห็นสุขยิ่งใหญ่ได้เพราะสละสุขเล็กน้อย ผู้มีปัญญาพึงสละสุขเล็กน้อยเพื่อสุขอันไพบูลย์", "บางครั้งการปล่อยสิ่งเล็ก ๆ นำความสุขที่ยิ่งใหญ่กว่ามาให้")}),
    ("Nagavagga", 23, 320, "patience,courage", "angry,anxious", {
        "en": ("I shall endure harsh words, as an elephant in battle endures an arrow; for many people are ill-mannered.", "Be strong and steady when others are unkind."),
        "hi": ("मैं कठोर वचन सह लूँगा जैसे संग्राम में हाथी तीर सह लेता है; क्योंकि संसार में बहुत लोग दुःशील होते हैं।", "जब दूसरे कठोर हों तब मज़बूत और स्थिर रहो।"),
        "th": ("เราจักอดทนต่อถ้อยคำล่วงเกิน ดุจช้างในสงครามอดทนต่อลูกศร เพราะคนส่วนมากเป็นคนทุศีล", "จงเข้มแข็งและมั่นคงเมื่อผู้อื่นไม่ใจดี")}),
    ("Nagavagga", 23, 328, "love,peace", "happy,sad", {
        "en": ("If you find a wise and steady companion, walk with them, joyful and mindful, overcoming every danger.", "A good, wise friend makes every journey easier."),
        "hi": ("यदि समझदार और स्थिर साथी मिल जाए तो उसके साथ प्रसन्न और सजग होकर सब संकट पार करते हुए चलो।", "एक अच्छा, समझदार मित्र हर यात्रा को आसान बना देता है।"),
        "th": ("หากได้สหายผู้มีปัญญา มีความประพฤติดี อยู่ร่วมกันได้ จงยินดี มีสติ ไปกับเขา ข้ามพ้นอันตรายทั้งปวง", "มิตรที่ดีและมีปัญญาทำให้ทุกการเดินทางง่ายขึ้น")}),
    ("Nagavagga", 23, 331, "gratitude,peace", "happy,sad", {
        "en": ("Good is a friend in need; good is contentment with whatever there is; good is merit when life is ending; good is giving up all suffering.", "Friends, contentment and kindness are real wealth."),
        "hi": ("आवश्यकता में मित्र अच्छा है; जो मिले उसमें संतोष अच्छा है; जीवन के अंत में पुण्य अच्छा है; सब दुखों का त्याग अच्छा है।", "मित्र, संतोष और दया ही असली धन हैं।"),
        "th": ("มีเพื่อนในยามต้องการเป็นสุข ความยินดีด้วยของที่มีเป็นสุข บุญในเวลาสิ้นชีวิตเป็นสุข การละทุกข์ทั้งปวงเป็นสุข", "เพื่อน ความพอใจ และความเมตตาคือทรัพย์แท้")}),
    ("Nagavagga", 23, 332, "love,gratitude", "happy,sad", {
        "en": ("Good in the world is caring for one's mother; good too is caring for one's father.", "Caring for our parents is a good and happy duty."),
        "hi": ("संसार में माता की सेवा अच्छी है; पिता की सेवा भी अच्छी है।", "माता-पिता की सेवा करना अच्छा और सुखद कर्तव्य है।"),
        "th": ("การบำรุงมารดาเป็นสุขในโลก การบำรุงบิดาก็เป็นสุขในโลก", "การดูแลพ่อแม่เป็นหน้าที่ที่ดีและนำสุขมาให้")}),
    ("Nagavagga", 23, 333, "wisdom,truth", "happy", {
        "en": ("Good is virtue lasting to old age; good is faith well rooted; good is the gaining of wisdom; good is not doing evil.", "A life of good values brings lasting peace."),
        "hi": ("बुढ़ापे तक शील अच्छा है; श्रद्धा का दृढ़ होना अच्छा है; प्रज्ञा की प्राप्ति अच्छी है; पाप न करना अच्छा है।", "अच्छे मूल्यों वाला जीवन स्थायी शांति देता है।"),
        "th": ("ศีลที่ตั้งมั่นจนถึงชราเป็นสุข ศรัทธาที่ตั้งมั่นเป็นสุข การได้ปัญญาเป็นสุข การไม่ทำบาปเป็นสุข", "ชีวิตที่มีคุณค่าที่ดีนำความสงบยั่งยืนมาให้")}),
    ("Bhikkhuvagga", 25, 361, "self-control,peace", "angry,anxious", {
        "en": ("Good is restraint in body, good is restraint in speech, good is restraint in mind; restraint in every way is good.", "Calm control of what we do, say and think is a gift."),
        "hi": ("शरीर का संयम अच्छा है, वाणी का संयम अच्छा है, मन का संयम अच्छा है; सर्वत्र संयम अच्छा है।", "हमारे करने, कहने और सोचने पर शांत नियंत्रण एक उपहार है।"),
        "th": ("การสำรวมกายดี การสำรวมวาจาดี การสำรวมใจดี การสำรวมในทุกทางดี", "การควบคุมสิ่งที่ทำ พูด และคิดอย่างสงบเป็นของขวัญ")}),
    ("Bhikkhuvagga", 25, 365, "gratitude,humility", "happy,sad", {
        "en": ("Do not despise what you have received, nor envy others; one who envies others does not attain peace of mind.", "Being content with your own share keeps your mind calm."),
        "hi": ("जो मिला है उसकी अवहेलना न करे, और दूसरों से ईर्ष्या न करे; ईर्ष्या करने वाले को समाधि नहीं मिलती।", "अपने हिस्से में संतुष्ट रहने से मन शांत रहता है।"),
        "th": ("อย่าดูหมิ่นสิ่งที่ตนได้ อย่าอิจฉาผู้อื่น ผู้อิจฉาผู้อื่นย่อมไม่ถึงความสงบแห่งจิต", "ความพอใจในส่วนของตนทำให้ใจสงบ")}),
    ("Bhikkhuvagga", 25, 368, "love,peace", "angry,happy", {
        "en": ("One who lives in loving-kindness and has faith in the teaching reaches the peaceful state, the stilling of all things, and happiness.", "Loving-kindness leads to deep peace."),
        "hi": ("जो मैत्री में रमता है और शासन में श्रद्धा रखता है, वह शांत पद, संस्कारों के उपशम और सुख को पाता है।", "मैत्री गहरी शांति तक ले जाती है।"),
        "th": ("ภิกษุผู้มีเมตตาเป็นเครื่องอยู่ เลื่อมใสในพระพุทธศาสนา ย่อมบรรลุบทอันสงบ เป็นที่ระงับสังขาร เป็นสุข", "เมตตานำไปสู่ความสงบอันลึกซึ้ง")}),
    ("Bhikkhuvagga", 25, 373, "peace,wisdom", "anxious,happy", {
        "en": ("For one who has entered an empty house, whose mind is calm, who sees clearly, there arises a joy beyond ordinary joy.", "Stillness and clear seeing give a joy that is rare and deep."),
        "hi": ("जो सूने घर में प्रवेश कर शांत चित्त से धर्म को ठीक से देखता है, उसे अमानवीय रति प्राप्त होती है।", "शांति और स्पष्ट दृष्टि वह ख़ुशी देती है जो दुर्लभ और गहरी है।"),
        "th": ("ภิกษุผู้เข้าไปสู่เรือนว่าง มีจิตสงบ เห็นธรรมโดยชอบ ย่อมได้รับความยินดีอันยิ่งกว่ามนุษย์", "ความสงบและการเห็นชัดให้ความสุขที่หาได้ยากและลึกซึ้ง")}),
    ("Bhikkhuvagga", 25, 378, "peace,self-control", "anxious,angry", {
        "en": ("One who is calm in body, calm in speech and calm in mind, well-composed, who has let go of worldly bait, is called peaceful.", "True peace shows in the way we move, speak and think."),
        "hi": ("जो काया से शांत, वाणी से शांत और मन से शांत है, सुसमाहित है और लोक के आमिष को छोड़ चुका है, वह उपशांत कहलाता है।", "सच्ची शांति हमारे चलने, बोलने और सोचने के ढंग में दिखाई देती है।"),
        "th": ("ภิกษุผู้สงบกาย สงบวาจา สงบใจ มีจิตตั้งมั่น ละอามิสในโลกได้ เรียกว่าผู้สงบระงับ", "ความสงบที่แท้ปรากฏในการเคลื่อนไหว การพูด และการคิดของเรา")}),
    ("Bhikkhuvagga", 25, 380, "self-control,wisdom", "anxious,sad", {
        "en": ("You yourself are your own protector; you yourself are your own refuge. So take good care of yourself, like a merchant caring for a fine horse.", "Take good care of yourself; you are your own best support."),
        "hi": ("आप ही अपने स्वामी हैं, आप ही अपनी शरण हैं; इसलिए स्वयं को ऐसे सँभालो जैसे व्यापारी अच्छे घोड़े को सँभालता है।", "अपना अच्छा ध्यान रखो; आप ही अपने सबसे बड़े सहारे हैं।"),
        "th": ("ตนแลเป็นที่พึ่งของตน ตนแลเป็นคติของตน เพราะฉะนั้นจงระวังตนให้ดี ดุจพ่อค้าม้าระวังม้าตัวดี", "จงดูแลตนเองให้ดี เพราะคุณคือที่พึ่งที่ดีที่สุดของตนเอง")}),
    ("Brahmanavagga", 26, 405, "compassion,peace", "angry,happy", {
        "en": ("Having laid aside the rod against all beings, moving or still, who neither kills nor causes to kill — that one I call a true person of holiness.", "Real goodness means never harming any living being."),
        "hi": ("जिसने चर-अचर सभी प्राणियों के प्रति दण्ड त्याग दिया है, जो न मारता है न मरवाता है, उसे मैं ब्राह्मण कहता हूँ।", "सच्ची भलाई किसी भी प्राणी को हानि न पहुँचाने में है।"),
        "th": ("ผู้วางอาชญาในสัตว์ทั้งหลาย ทั้งที่สั่นหวาดกลัวและมั่นคง ไม่ฆ่าเอง ไม่ใช้ให้ผู้อื่นฆ่า เราเรียกผู้นั้นว่าพราหมณ์", "ความดีที่แท้คือการไม่เบียดเบียนสิ่งมีชีวิตใด ๆ")}),
    ("Brahmanavagga", 26, 406, "peace,compassion", "angry,anxious", {
        "en": ("Friendly among the hostile, peaceful among the violent, unattached among the attached — that one I call a true person of holiness.", "Stay kind and calm even when everyone around is not."),
        "hi": ("जो विरोधियों के बीच मित्र, हिंसकों के बीच शांत और आसक्तों के बीच अनासक्त है, उसे मैं ब्राह्मण कहता हूँ।", "जब आसपास सब उलटे हों तब भी दयालु और शांत रहो।"),
        "th": ("ผู้เป็นมิตรในหมู่ผู้เป็นศัตรู สงบในหมู่ผู้ถืออาชญา ไม่ยึดมั่นในหมู่ผู้ยึดมั่น เราเรียกผู้นั้นว่าพราหมณ์", "จงใจดีและสงบ แม้คนรอบข้างจะไม่เป็นเช่นนั้น")}),
    ("Brahmanavagga", 26, 408, "truth,compassion", "angry,happy", {
        "en": ("One who speaks gentle, helpful and true words, offending no one — that one I call a true person of holiness.", "Speak words that are gentle, useful and true."),
        "hi": ("जो अकर्कश, सार्थक और सत्य वाणी बोलता है, जिससे किसी को पीड़ा नहीं होती, उसे मैं ब्राह्मण कहता हूँ।", "ऐसे शब्द बोलो जो कोमल, उपयोगी और सच्चे हों।"),
        "th": ("ผู้กล่าววาจาไม่หยาบ ให้รู้ความ เป็นความจริง ไม่ขัดเคืองใคร เราเรียกผู้นั้นว่าพราหมณ์", "จงพูดถ้อยคำที่อ่อนโยน มีประโยชน์ และเป็นจริง")}),
    ("Pupphavagga", 4, 50, "humility,wisdom", "angry,happy", {
        "en": ("Do not look at the faults of others, at what they have done or left undone; look rather at what you yourself have done and left undone.", "Check your own actions before noticing other people's."),
        "hi": ("दूसरों के दोषों को, उन्होंने क्या किया और क्या नहीं किया, मत देखो; अपने ही किए और अनकिए को देखो।", "दूसरों के कामों को देखने से पहले अपने कामों को परख लो।"),
        "th": ("อย่าเพ่งโทษผู้อื่น อย่าดูสิ่งที่เขาทำและไม่ได้ทำ จงดูแต่กรรมของตน ที่ทำแล้วและยังไม่ได้ทำ", "ตรวจดูการกระทำของตนก่อนสังเกตผู้อื่น")}),
    ("Brahmanavagga", 26, 400, "self-control,humility", "angry,happy", {
        "en": ("Free from anger, devoted, virtuous, without craving, self-controlled, in their last body — that one I call a true person of holiness.", "A calm, disciplined and kind heart is the mark of true goodness."),
        "hi": ("जो क्रोध से रहित, व्रती, शीलवान, तृष्णा रहित और संयमी है, जिसका यह अंतिम शरीर है, उसे मैं ब्राह्मण कहता हूँ।", "शांत, अनुशासित और दयालु हृदय सच्ची भलाई की पहचान है।"),
        "th": ("ผู้ไม่โกรธ มีวัตร มีศีล ไม่มีตัณหา ฝึกตนแล้ว มีกายสุดท้าย เราเรียกผู้นั้นว่าพราหมณ์", "ใจที่สงบ มีวินัย และเมตตา คือเครื่องหมายของความดีแท้")}),
]
