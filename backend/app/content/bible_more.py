"""Bible — 69 more verses (v2) × en / hi / es, bringing Christianity to 100 verses per language.

English: King James Version (public domain). Hindi and Spanish: WisdomOne's plain-language renderings of the same
verses (in the spirit of the public-domain Hindi and Reina-Valera Bibles) — please have them proof-read by native
speakers before a public release. Row format as in bible.py.
"""

VERSES = [
    ("Psalms", 4, 8, "peace,faith", "anxious,sad", {
        "en": ("I will both lay me down in peace, and sleep: for thou, LORD, only makest me dwell in safety.", "You can rest easy at night, because you are looked after."),
        "hi": ("मैं शांति से लेट जाऊँगा और सो जाऊँगा; क्योंकि हे यहोवा, केवल तू ही मुझे सुरक्षित रखता है।", "आप रात को चैन से सो सकते हैं, क्योंकि कोई आपकी देखभाल करता है।"),
        "es": ("En paz me acostaré, y asimismo dormiré; porque solo tú, Jehová, me haces vivir confiado.", "Puedes descansar tranquilo de noche, porque alguien cuida de ti.")}),
    ("Psalms", 27, 1, "faith,courage", "anxious", {
        "en": ("The LORD is my light and my salvation; whom shall I fear? the LORD is the strength of my life; of whom shall I be afraid?", "With God as your light, there is nothing left to be scared of."),
        "hi": ("यहोवा मेरी ज्योति और मेरा उद्धार है; मैं किससे डरूँ? यहोवा मेरे जीवन का दृढ़ गढ़ है; मैं किससे भयभीत होऊँ?", "जब ईश्वर आपकी रोशनी हैं, तो डरने की कोई बात नहीं रहती।"),
        "es": ("Jehová es mi luz y mi salvación; ¿de quién temeré? Jehová es la fortaleza de mi vida; ¿de quién he de atemorizarme?", "Si Dios es tu luz, no queda nada que temer.")}),
    ("Psalms", 27, 14, "patience,hope", "anxious,sad", {
        "en": ("Wait on the LORD: be of good courage, and he shall strengthen thine heart: wait, I say, on the LORD.", "Be patient and brave; strength comes to those who wait with trust."),
        "hi": ("यहोवा की बाट जोहता रह; दृढ़ हो और तेरा हृदय मज़बूत हो; हाँ, यहोवा ही की बाट जोहता रह।", "धीरज और हिम्मत रखो; भरोसे के साथ इंतज़ार करने वालों को ताक़त मिलती है।"),
        "es": ("Aguarda a Jehová; esfuérzate, y aliéntese tu corazón; sí, espera a Jehová.", "Ten paciencia y valor; la fuerza llega a quien espera con confianza.")}),
    ("Psalms", 30, 5, "hope,patience", "sad,anxious", {
        "en": ("Weeping may endure for a night, but joy cometh in the morning.", "Sad nights do not last forever; morning always comes."),
        "hi": ("रात भर रोना रहता है, परन्तु भोर को आनन्द आता है।", "उदास रातें हमेशा नहीं रहतीं; सुबह ज़रूर आती है।"),
        "es": ("Por la noche durará el lloro, y a la mañana vendrá la alegría.", "Las noches tristes no duran para siempre; la mañana siempre llega.")}),
    ("Psalms", 31, 24, "courage,hope", "anxious,sad", {
        "en": ("Be of good courage, and he shall strengthen your heart, all ye that hope in the LORD.", "Keep hoping and stay brave, and your heart will grow stronger."),
        "hi": ("हे यहोवा पर आशा रखनेवालो, हियाव बाँधो और तुम्हारा हृदय दृढ़ रहे।", "आशा रखो और हिम्मत बनाए रखो, तुम्हारा मन मज़बूत होगा।"),
        "es": ("Esforzaos todos vosotros los que esperáis en Jehová, y tome aliento vuestro corazón.", "Sigue esperando y sé valiente, y tu corazón se fortalecerá.")}),
    ("Psalms", 37, 4, "faith,gratitude", "happy", {
        "en": ("Delight thyself also in the LORD; and he shall give thee the desires of thine heart.", "Find your joy in goodness, and your heart's deepest wishes will be met."),
        "hi": ("यहोवा को अपने सुख का मूल जान, और वह तेरे मनोरथों को पूरा करेगा।", "भलाई में अपनी ख़ुशी ढूँढ़ो, तो तुम्हारे मन की गहरी इच्छाएँ पूरी होंगी।"),
        "es": ("Deléitate asimismo en Jehová, y él te concederá las peticiones de tu corazón.", "Encuentra tu alegría en lo bueno y los deseos de tu corazón se cumplirán.")}),
    ("Psalms", 37, 5, "faith,hope", "anxious", {
        "en": ("Commit thy way unto the LORD; trust also in him; and he shall bring it to pass.", "Hand your plans over with trust, and let things unfold."),
        "hi": ("अपना मार्ग यहोवा पर छोड़ दे; और उस पर भरोसा रख, वही पूरा करेगा।", "अपनी योजनाएँ भरोसे के साथ सौंप दो और होने दो।"),
        "es": ("Encomienda a Jehová tu camino, y confía en él; y él hará.", "Entrega tus planes con confianza y deja que todo se desarrolle.")}),
    ("Psalms", 37, 7, "patience,peace", "anxious,angry", {
        "en": ("Rest in the LORD, and wait patiently for him: fret not thyself because of him who prospereth in his way.", "Stay calm and patient, and do not get upset over what others have."),
        "hi": ("यहोवा के सामने चुपचाप रह, और धीरज से उसकी प्रतीक्षा कर; जो अपने मार्ग में सफल होता है, उस पर कुढ़ना मत।", "शांत और धीरजवान रहो, और दूसरों के पास जो है उस पर परेशान मत हो।"),
        "es": ("Guarda silencio ante Jehová, y espera en él; no te alteres con motivo del que prospera en su camino.", "Mantén la calma y la paciencia, y no te irrites por lo que otros tienen.")}),
    ("Psalms", 46, 10, "peace,faith", "anxious,angry", {
        "en": ("Be still, and know that I am God.", "Stop for a moment, be quiet inside, and remember you are not alone."),
        "hi": ("चुप हो जाओ, और जान लो कि मैं ही परमेश्वर हूँ।", "एक पल रुको, भीतर से शांत हो जाओ और याद रखो कि तुम अकेले नहीं हो।"),
        "es": ("Estad quietos, y conoced que yo soy Dios.", "Detente un momento, calla por dentro y recuerda que no estás solo.")}),
    ("Psalms", 51, 10, "humility,hope", "sad,angry", {
        "en": ("Create in me a clean heart, O God; and renew a right spirit within me.", "It is a good prayer to ask for a fresh, clean start inside."),
        "hi": ("हे परमेश्वर, मेरे भीतर शुद्ध मन उत्पन्न कर, और मेरे अन्दर एक स्थिर आत्मा नये सिरे से उत्पन्न कर।", "भीतर से एक नई, साफ़ शुरुआत माँगना एक अच्छी प्रार्थना है।"),
        "es": ("Crea en mí, oh Dios, un corazón limpio, y renueva un espíritu recto dentro de mí.", "Pedir un comienzo nuevo y limpio por dentro es una buena oración.")}),
    ("Psalms", 55, 22, "faith,peace", "anxious,sad", {
        "en": ("Cast thy burden upon the LORD, and he shall sustain thee: he shall never suffer the righteous to be moved.", "You do not have to carry everything alone; share the weight."),
        "hi": ("अपना बोझ यहोवा पर डाल दे, वह तुझे सम्भालेगा; वह धर्मी को कभी टलने न देगा।", "आपको सब कुछ अकेले ढोना नहीं है; बोझ बाँट लीजिए।"),
        "es": ("Echa sobre Jehová tu carga, y él te sustentará; no dejará para siempre caído al justo.", "No tienes que cargar con todo tú solo; comparte el peso.")}),
    ("Psalms", 90, 12, "wisdom,gratitude", "happy,anxious", {
        "en": ("So teach us to number our days, that we may apply our hearts unto wisdom.", "Remember how precious each day is, and spend it wisely."),
        "hi": ("हमें अपने दिन गिनना सिखा, ताकि हम बुद्धिमान हृदय प्राप्त करें।", "याद रखो कि हर दिन कितना क़ीमती है, और उसे समझदारी से बिताओ।"),
        "es": ("Enséñanos de tal modo a contar nuestros días, que traigamos al corazón sabiduría.", "Recuerda lo valioso de cada día y úsalo con sabiduría.")}),
    ("Psalms", 91, 1, "faith,peace", "anxious", {
        "en": ("He that dwelleth in the secret place of the most High shall abide under the shadow of the Almighty.", "A heart that rests in God finds a safe, quiet shelter."),
        "hi": ("जो परमप्रधान के छाए हुए स्थान में बैठा रहता है, वह सर्वशक्तिमान की छाया में ठिकाना पाएगा।", "जो मन ईश्वर में विश्राम करता है, उसे सुरक्षित और शांत आश्रय मिलता है।"),
        "es": ("El que habita al abrigo del Altísimo morará bajo la sombra del Omnipotente.", "El corazón que descansa en Dios encuentra un refugio seguro y tranquilo.")}),
    ("Psalms", 100, 4, "gratitude", "happy", {
        "en": ("Enter into his gates with thanksgiving, and into his courts with praise: be thankful unto him, and bless his name.", "Begin with thanks and a happy heart, and everything feels brighter."),
        "hi": ("धन्यवाद करते हुए उसके फाटकों में, और स्तुति करते हुए उसके आँगनों में प्रवेश करो; उसका धन्यवाद करो और उसके नाम को धन्य कहो।", "धन्यवाद और ख़ुश दिल से शुरुआत करो, तो सब कुछ उजला लगता है।"),
        "es": ("Entrad por sus puertas con acción de gracias, por sus atrios con alabanza; alabadle, bendecid su nombre.", "Empieza con gracias y un corazón alegre, y todo se ve más luminoso.")}),
    ("Psalms", 103, 8, "compassion,patience", "angry,sad", {
        "en": ("The LORD is merciful and gracious, slow to anger, and plenteous in mercy.", "Kindness and patience are at the heart of a good God."),
        "hi": ("यहोवा दयालु और अनुग्रहकारी है, विलम्ब से क्रोध करनेवाला और अति करुणामय है।", "दया और धैर्य एक भले ईश्वर के हृदय में हैं।"),
        "es": ("Misericordioso y clemente es Jehová; lento para la ira, y grande en misericordia.", "La bondad y la paciencia están en el corazón de un Dios bueno.")}),
    ("Psalms", 119, 105, "wisdom,hope", "anxious,sad", {
        "en": ("Thy word is a lamp unto my feet, and a light unto my path.", "Wise words are like a small light that shows the next step."),
        "hi": ("तेरा वचन मेरे पाँव के लिये दीपक, और मेरे मार्ग के लिये उजियाला है।", "समझदारी भरे शब्द एक छोटे दीये जैसे हैं जो अगला क़दम दिखाते हैं।"),
        "es": ("Lámpara es a mis pies tu palabra, y lumbrera a mi camino.", "Las palabras sabias son como una luz pequeña que muestra el siguiente paso.")}),
    ("Psalms", 126, 5, "hope,patience", "sad,happy", {
        "en": ("They that sow in tears shall reap in joy.", "Hard work done with hope today will bring joy later."),
        "hi": ("जो आँसू बहाते हुए बोते हैं, वे जयजयकार करते हुए लवने पाएँगे।", "आज आशा के साथ की गई मेहनत आगे चलकर ख़ुशी लाती है।"),
        "es": ("Los que sembraron con lágrimas, con regocijo segarán.", "El esfuerzo hecho hoy con esperanza traerá alegría después.")}),
    ("Psalms", 133, 1, "love,peace", "happy,angry", {
        "en": ("Behold, how good and how pleasant it is for brethren to dwell together in unity!", "Living together in peace is a joy for everyone."),
        "hi": ("देखो, यह क्या ही भली और मनोहर बात है कि भाई लोग आपस में मिले रहें!", "मिल-जुलकर शांति से रहना सबके लिए ख़ुशी की बात है।"),
        "es": ("¡Mirad cuán bueno y cuán delicioso es habitar los hermanos juntos en armonía!", "Vivir juntos en paz es una alegría para todos.")}),
    ("Proverbs", 3, 6, "faith,wisdom", "anxious", {
        "en": ("In all thy ways acknowledge him, and he shall direct thy paths.", "Keep God in mind in everything you do, and the way will become clear."),
        "hi": ("अपने सब कामों में उसको स्मरण कर, तो वह तेरे लिये सीधा मार्ग निकालेगा।", "हर काम में ईश्वर को याद रखो, तो रास्ता साफ़ होता जाएगा।"),
        "es": ("Reconócelo en todos tus caminos, y él enderezará tus veredas.", "Ten presente a Dios en todo lo que hagas y el camino se aclarará.")}),
    ("Proverbs", 4, 23, "wisdom,self-control", "anxious,angry", {
        "en": ("Keep thy heart with all diligence; for out of it are the issues of life.", "Take care of your heart and thoughts, because they shape your life."),
        "hi": ("सब से अधिक अपने मन की रक्षा कर; क्योंकि जीवन का मूल स्रोत वही है।", "अपने मन और विचारों का ध्यान रखो, क्योंकि वही तुम्हारे जीवन को आकार देते हैं।"),
        "es": ("Sobre toda cosa guardada, guarda tu corazón; porque de él mana la vida.", "Cuida tu corazón y tus pensamientos, porque ellos dan forma a tu vida.")}),
    ("Proverbs", 10, 12, "love,forgiveness", "angry,sad", {
        "en": ("Hatred stirreth up strifes: but love covereth all sins.", "Hate makes fights bigger; love helps us forgive."),
        "hi": ("बैर से झगड़े उत्पन्न होते हैं, परन्तु प्रेम से सब अपराध ढँप जाते हैं।", "नफ़रत झगड़े बढ़ाती है; प्रेम हमें माफ़ करना सिखाता है।"),
        "es": ("El odio despierta rencillas; pero el amor cubrirá todas las faltas.", "El odio agranda las peleas; el amor nos ayuda a perdonar.")}),
    ("Proverbs", 12, 25, "compassion,hope", "sad,anxious", {
        "en": ("Heaviness in the heart of man maketh it stoop: but a good word maketh it glad.", "A kind word can lift someone's heavy heart."),
        "hi": ("उदासी मनुष्य के हृदय को दबा देती है, परन्तु भली बात से वह आनन्दित होता है।", "एक अच्छा शब्द किसी के भारी मन को हल्का कर सकता है।"),
        "es": ("La congoja en el corazón del hombre lo abate; mas la buena palabra lo alegra.", "Una palabra amable puede aliviar un corazón pesado.")}),
    ("Proverbs", 16, 3, "faith,wisdom", "anxious,happy", {
        "en": ("Commit thy works unto the LORD, and thy thoughts shall be established.", "Offer your work with a sincere heart, and your mind will settle."),
        "hi": ("अपने कामों को यहोवा के हाथ में सौंप दे, तो तेरी योजनाएँ सफल होंगी।", "अपना काम सच्चे मन से सौंप दो, तो मन स्थिर हो जाएगा।"),
        "es": ("Encomienda a Jehová tus obras, y tus pensamientos serán afirmados.", "Ofrece tu trabajo con sinceridad y tu mente se asentará.")}),
    ("Proverbs", 17, 17, "love,compassion", "sad,happy", {
        "en": ("A friend loveth at all times, and a brother is born for adversity.", "A true friend stays with you in easy times and hard times."),
        "hi": ("मित्र सब समयों में प्रेम रखता है, और विपत्ति के दिन भाई बन जाता है।", "सच्चा मित्र आसान और कठिन, दोनों समय आपके साथ रहता है।"),
        "es": ("En todo tiempo ama el amigo, y es como un hermano en tiempo de angustia.", "Un amigo de verdad está contigo en los tiempos fáciles y en los difíciles.")}),
    ("Proverbs", 18, 10, "faith,courage", "anxious", {
        "en": ("The name of the LORD is a strong tower: the righteous runneth into it, and is safe.", "When you feel afraid, turning to God is like running to a strong tower."),
        "hi": ("यहोवा का नाम दृढ़ गढ़ है; धर्मी उसमें भागकर सुरक्षित रहता है।", "डर लगे तो ईश्वर की ओर जाना एक मज़बूत मीनार की ओर भागने जैसा है।"),
        "es": ("Torre fuerte es el nombre de Jehová; a él correrá el justo, y será levantado.", "Cuando tengas miedo, acudir a Dios es como correr a una torre fuerte.")}),
    ("Proverbs", 19, 11, "self-control,forgiveness", "angry", {
        "en": ("The discretion of a man deferreth his anger; and it is his glory to pass over a transgression.", "Wise people take a breath before getting angry, and it is noble to let small wrongs go."),
        "hi": ("मनुष्य की बुद्धि उसे विलम्ब से क्रोध करनेवाला बनाती है, और अपराध को भुला देना उसकी शोभा है।", "समझदार लोग क्रोध से पहले रुकते हैं, और छोटी ग़लतियों को जाने देना महानता है।"),
        "es": ("La cordura del hombre detiene su furor, y su honra es pasar por alto la ofensa.", "Los sabios respiran antes de enojarse, y es noble dejar pasar las faltas pequeñas.")}),
    ("Ecclesiastes", 3, 1, "patience,wisdom", "anxious,sad", {
        "en": ("To every thing there is a season, and a time to every purpose under the heaven.", "Everything has its right time, so be patient with life's seasons."),
        "hi": ("हर एक बात का एक अवसर और आकाश के नीचे हर एक काम का एक समय है।", "हर बात का अपना सही समय है, इसलिए जीवन के मौसमों के साथ धैर्य रखो।"),
        "es": ("Todo tiene su tiempo, y todo lo que se quiere debajo del cielo tiene su hora.", "Todo tiene su momento justo; ten paciencia con las estaciones de la vida.")}),
    ("Ecclesiastes", 4, 9, "love,compassion", "sad,happy", {
        "en": ("Two are better than one; because they have a good reward for their labour.", "Working together makes life easier and better."),
        "hi": ("एक से दो अच्छे हैं, क्योंकि उनके परिश्रम का अच्छा फल मिलता है।", "मिलकर काम करने से जीवन आसान और बेहतर बनता है।"),
        "es": ("Mejores son dos que uno; porque tienen mejor paga de su trabajo.", "Trabajar juntos hace la vida más fácil y mejor.")}),
    ("Isaiah", 40, 31, "hope,patience", "sad,anxious", {
        "en": ("They that wait upon the LORD shall renew their strength; they shall mount up with wings as eagles; they shall run, and not be weary; and they shall walk, and not faint.", "Trust gives tired people fresh strength to keep going."),
        "hi": ("जो यहोवा की बाट जोहते हैं, वे नया बल प्राप्त करते जाएँगे; वे उकाबों की नाईं उड़ेंगे; वे दौड़ेंगे और श्रमित न होंगे, चलेंगे और थकित न होंगे।", "भरोसा थके हुए लोगों को आगे बढ़ने की नई ताक़त देता है।"),
        "es": ("Los que esperan a Jehová tendrán nuevas fuerzas; levantarán alas como las águilas; correrán, y no se cansarán; caminarán, y no se fatigarán.", "La confianza da fuerzas nuevas a los cansados para seguir adelante.")}),
    ("Isaiah", 43, 2, "courage,faith", "anxious,sad", {
        "en": ("When thou passest through the waters, I will be with thee; and through the rivers, they shall not overflow thee.", "In every hard time, you are not alone."),
        "hi": ("जब तू जल में होकर जाए, मैं तेरे संग रहूँगा; और जब तू नदियों में होकर चले, तब वे तुझे न डुबा सकेंगी।", "हर कठिन समय में आप अकेले नहीं हैं।"),
        "es": ("Cuando pases por las aguas, yo estaré contigo; y si por los ríos, no te anegarán.", "En cada tiempo difícil no estás solo.")}),
    ("Jeremiah", 29, 11, "hope,faith", "sad,anxious", {
        "en": ("For I know the thoughts that I think toward you, saith the LORD, thoughts of peace, and not of evil, to give you an expected end.", "There are good plans for your future, and hope is always allowed."),
        "hi": ("क्योंकि यहोवा की यह वाणी है, कि जो विचार मैं तुम्हारे विषय करता हूँ, वे हानि के नहीं, वरन् कुशल के हैं, ताकि तुम्हारा भविष्य और आशा बनी रहे।", "आपके भविष्य के लिए अच्छी योजनाएँ हैं, और आशा रखना हमेशा ठीक है।"),
        "es": ("Porque yo sé los pensamientos que tengo acerca de vosotros, dice Jehová, pensamientos de paz, y no de mal, para daros el fin que esperáis.", "Hay buenos planes para tu futuro, y siempre se puede tener esperanza.")}),
    ("Lamentations", 3, 23, "gratitude,hope", "happy,sad", {
        "en": ("They are new every morning: great is thy faithfulness.", "Every morning is a fresh start with new kindness."),
        "hi": ("वे प्रति भोर नये होते रहते हैं; तेरी सच्चाई महान है।", "हर सुबह नई दया के साथ एक नई शुरुआत है।"),
        "es": ("Nuevas son cada mañana; grande es tu fidelidad.", "Cada mañana es un nuevo comienzo con bondad renovada.")}),
    ("Micah", 6, 8, "humility,compassion", "happy,angry", {
        "en": ("What doth the LORD require of thee, but to do justly, and to love mercy, and to walk humbly with thy God?", "Be fair, be kind, and be humble — that is what matters."),
        "hi": ("यहोवा तुझ से इसके सिवाय और क्या चाहता है कि तू न्याय से काम करे, और कृपा से प्रीति रखे, और अपने परमेश्वर के साथ नम्रता से चले?", "न्यायी बनो, दयालु बनो और विनम्र रहो — यही सबसे ज़रूरी है।"),
        "es": ("¿Qué pide Jehová de ti, sino hacer justicia, y amar misericordia, y humillarte ante tu Dios?", "Sé justo, sé amable y sé humilde: eso es lo que importa.")}),
    ("Matthew", 5, 5, "humility,peace", "angry,happy", {
        "en": ("Blessed are the meek: for they shall inherit the earth.", "Gentle people end up with a peaceful life."),
        "hi": ("धन्य हैं वे, जो नम्र हैं, क्योंकि वे पृथ्वी के अधिकारी होंगे।", "कोमल स्वभाव के लोग शांत जीवन पाते हैं।"),
        "es": ("Bienaventurados los mansos, porque ellos recibirán la tierra por heredad.", "Las personas gentiles terminan con una vida en paz.")}),
    ("Matthew", 5, 7, "compassion,forgiveness", "angry,happy", {
        "en": ("Blessed are the merciful: for they shall obtain mercy.", "When you are kind to others, kindness comes back to you."),
        "hi": ("धन्य हैं वे, जो दयावन्त हैं, क्योंकि उन पर दया की जाएगी।", "जब आप दूसरों पर दया करते हैं, तो दया आपके पास लौटकर आती है।"),
        "es": ("Bienaventurados los misericordiosos, porque ellos alcanzarán misericordia.", "Cuando eres amable con otros, la amabilidad vuelve a ti.")}),
    ("Matthew", 5, 9, "peace,compassion", "angry,happy", {
        "en": ("Blessed are the peacemakers: for they shall be called the children of God.", "People who bring calm and settle quarrels are truly blessed."),
        "hi": ("धन्य हैं वे, जो मेल करवानेवाले हैं, क्योंकि वे परमेश्वर के पुत्र कहलाएँगे।", "जो लोग शांति लाते हैं और झगड़े सुलझाते हैं, वे सचमुच धन्य हैं।"),
        "es": ("Bienaventurados los pacificadores, porque ellos serán llamados hijos de Dios.", "Quienes traen calma y arreglan las peleas son verdaderamente bendecidos.")}),
    ("Matthew", 6, 14, "forgiveness", "angry,sad", {
        "en": ("For if ye forgive men their trespasses, your heavenly Father will also forgive you.", "Forgiving others opens the door to being forgiven."),
        "hi": ("क्योंकि यदि तुम मनुष्यों के अपराध क्षमा करोगे, तो तुम्हारा स्वर्गीय पिता भी तुम्हें क्षमा करेगा।", "दूसरों को माफ़ करने से माफ़ी पाने का रास्ता खुलता है।"),
        "es": ("Porque si perdonáis a los hombres sus ofensas, os perdonará también a vosotros vuestro Padre celestial.", "Perdonar a otros abre la puerta a ser perdonado.")}),
    ("Matthew", 6, 21, "wisdom,truth", "happy,anxious", {
        "en": ("For where your treasure is, there will your heart be also.", "What you value most shapes where your heart goes."),
        "hi": ("क्योंकि जहाँ तुम्हारा धन है, वहीं तुम्हारा मन भी लगा रहेगा।", "आप जिसे सबसे क़ीमती मानते हैं, मन वहीं जाता है।"),
        "es": ("Porque donde esté vuestro tesoro, allí estará también vuestro corazón.", "Lo que más valoras guía hacia dónde va tu corazón.")}),
    ("Matthew", 6, 33, "faith,wisdom", "anxious", {
        "en": ("But seek ye first the kingdom of God, and his righteousness; and all these things shall be added unto you.", "Put goodness first, and the other things will fall into place."),
        "hi": ("इसलिये पहले तुम उसके राज्य और धर्म की खोज करो, तो ये सब वस्तुएँ तुम्हें दी जाएँगी।", "पहले भलाई को रखो, तो बाक़ी चीज़ें अपनी जगह पर आ जाएँगी।"),
        "es": ("Mas buscad primeramente el reino de Dios y su justicia, y todas estas cosas os serán añadidas.", "Pon primero lo bueno y lo demás se acomodará.")}),
    ("Matthew", 7, 7, "hope,faith", "anxious,sad", {
        "en": ("Ask, and it shall be given you; seek, and ye shall find; knock, and it shall be opened unto you.", "Keep asking and trying; good doors do open."),
        "hi": ("माँगो, तो तुम्हें दिया जाएगा; ढूँढ़ो, तो तुम पाओगे; खटखटाओ, तो तुम्हारे लिये खोला जाएगा।", "माँगते और कोशिश करते रहो; अच्छे दरवाज़े खुलते हैं।"),
        "es": ("Pedid, y se os dará; buscad, y hallaréis; llamad, y se os abrirá.", "Sigue pidiendo y probando; las buenas puertas se abren.")}),
    ("Matthew", 11, 29, "humility,peace", "sad,anxious", {
        "en": ("Take my yoke upon you, and learn of me; for I am meek and lowly in heart: and ye shall find rest unto your souls.", "Gentleness and humility bring rest to a tired soul."),
        "hi": ("मेरा जूआ अपने ऊपर उठा लो, और मुझ से सीखो; क्योंकि मैं नम्र और मन में दीन हूँ; और तुम अपने मन में विश्राम पाओगे।", "कोमलता और विनम्रता थकी हुई आत्मा को विश्राम देती हैं।"),
        "es": ("Llevad mi yugo sobre vosotros, y aprended de mí, que soy manso y humilde de corazón; y hallaréis descanso para vuestras almas.", "La mansedumbre y la humildad dan descanso a un alma cansada.")}),
    ("Matthew", 19, 26, "hope,faith", "sad,anxious", {
        "en": ("With men this is impossible; but with God all things are possible.", "Things that seem impossible to us can still become possible."),
        "hi": ("मनुष्यों से तो यह नहीं हो सकता, परन्तु परमेश्वर से सब कुछ हो सकता है।", "जो हमें असंभव लगता है, वह भी संभव हो सकता है।"),
        "es": ("Para los hombres esto es imposible; mas para Dios todo es posible.", "Lo que nos parece imposible todavía puede hacerse posible.")}),
    ("Mark", 9, 23, "faith,hope", "anxious,sad", {
        "en": ("If thou canst believe, all things are possible to him that believeth.", "Belief gives us courage to try what feels too big."),
        "hi": ("यदि तू कर सकता है! विश्वास करनेवाले के लिये सब कुछ हो सकता है।", "विश्वास हमें बड़े काम करने की हिम्मत देता है।"),
        "es": ("Si puedes creer, al que cree todo le es posible.", "La fe nos da valor para intentar lo que parece demasiado grande.")}),
    ("Luke", 6, 36, "compassion,forgiveness", "angry,happy", {
        "en": ("Be ye therefore merciful, as your Father also is merciful.", "Treat others with the same kindness you hope to receive."),
        "hi": ("जैसा तुम्हारा पिता दयावन्त है, वैसे ही तुम भी दयावन्त बनो।", "दूसरों के साथ वैसी ही दया करो जैसी तुम पाना चाहते हो।"),
        "es": ("Sed, pues, misericordiosos, como también vuestro Padre es misericordioso.", "Trata a los demás con la misma bondad que esperas recibir.")}),
    ("John", 8, 12, "hope,wisdom", "sad,anxious", {
        "en": ("I am the light of the world: he that followeth me shall not walk in darkness, but shall have the light of life.", "Following goodness is like carrying a light in the dark."),
        "hi": ("जगत की ज्योति मैं हूँ; जो मेरे पीछे हो लेगा, वह अंधकार में न चलेगा, परन्तु जीवन की ज्योति पाएगा।", "भलाई पर चलना अँधेरे में दीया लेकर चलने जैसा है।"),
        "es": ("Yo soy la luz del mundo; el que me sigue, no andará en tinieblas, sino que tendrá la luz de la vida.", "Seguir lo bueno es como llevar una luz en la oscuridad.")}),
    ("John", 8, 32, "truth,wisdom", "happy,angry", {
        "en": ("And ye shall know the truth, and the truth shall make you free.", "Honesty and understanding set the heart free."),
        "hi": ("और तुम सत्य को जानोगे, और सत्य तुम्हें स्वतंत्र करेगा।", "सच्चाई और समझ मन को आज़ाद करती हैं।"),
        "es": ("y conoceréis la verdad, y la verdad os hará libres.", "La honestidad y la comprensión liberan el corazón.")}),
    ("John", 15, 12, "love,compassion", "happy,sad", {
        "en": ("This is my commandment, That ye love one another, as I have loved you.", "Love each other the way you have been loved."),
        "hi": ("मेरी आज्ञा यह है कि जैसा मैंने तुम से प्रेम रखा, वैसा ही तुम भी एक दूसरे से प्रेम रखो।", "एक-दूसरे से वैसे ही प्रेम करो जैसे तुम से प्रेम किया गया है।"),
        "es": ("Este es mi mandamiento: Que os améis unos a otros, como yo os he amado.", "Ámense unos a otros como han sido amados.")}),
    ("John", 16, 33, "peace,courage", "anxious,sad", {
        "en": ("These things I have spoken unto you, that in me ye might have peace. In the world ye shall have tribulation: but be of good cheer; I have overcome the world.", "Life has troubles, but peace and courage are still possible."),
        "hi": ("मैंने ये बातें तुम से इसलिये कही हैं कि तुम्हें मुझ में शान्ति मिले। संसार में तुम्हें क्लेश होगा, परन्तु ढाढ़स बाँधो, मैंने संसार को जीत लिया है।", "जीवन में कठिनाइयाँ हैं, फिर भी शांति और हिम्मत संभव है।"),
        "es": ("Estas cosas os he hablado para que en mí tengáis paz. En el mundo tendréis aflicción; pero confiad, yo he vencido al mundo.", "La vida tiene problemas, pero aún es posible tener paz y valor.")}),
    ("Romans", 8, 28, "faith,hope", "sad,anxious", {
        "en": ("And we know that all things work together for good to them that love God, to them who are the called according to his purpose.", "Even hard things can turn into something good in time."),
        "hi": ("और हम जानते हैं कि जो लोग परमेश्वर से प्रेम रखते हैं, उनके लिये सब बातें मिलकर भलाई ही को उत्पन्न करती हैं।", "कठिन बातें भी समय के साथ भलाई में बदल सकती हैं।"),
        "es": ("Y sabemos que a los que aman a Dios, todas las cosas les ayudan a bien.", "Incluso las cosas difíciles pueden convertirse en algo bueno con el tiempo.")}),
    ("Romans", 8, 31, "courage,faith", "anxious", {
        "en": ("If God be for us, who can be against us?", "With goodness on your side, you can face anything."),
        "hi": ("यदि परमेश्वर हमारी ओर है, तो हमारा विरोधी कौन हो सकता है?", "जब भलाई आपके साथ है, तो आप किसी भी चीज़ का सामना कर सकते हैं।"),
        "es": ("Si Dios es por nosotros, ¿quién contra nosotros?", "Con la bondad de tu lado, puedes enfrentarte a cualquier cosa.")}),
    ("Romans", 12, 18, "peace,patience", "angry", {
        "en": ("If it be possible, as much as lieth in you, live peaceably with all men.", "Do your part to live in peace with everyone."),
        "hi": ("जहाँ तक हो सके, तुम अपनी ओर से सब मनुष्यों के साथ मेल-मिलाप रखो।", "सबके साथ शांति से रहने के लिए अपनी ओर से पूरी कोशिश करो।"),
        "es": ("Si es posible, en cuanto dependa de vosotros, estad en paz con todos los hombres.", "Haz tu parte para vivir en paz con todos.")}),
    ("Romans", 15, 13, "hope,peace", "happy,sad", {
        "en": ("Now the God of hope fill you with all joy and peace in believing, that ye may abound in hope.", "May hope fill your heart with joy and peace."),
        "hi": ("आशा का परमेश्वर तुम्हें विश्वास करने में सब प्रकार के आनन्द और शान्ति से भर दे, कि तुम्हारी आशा बढ़ती जाए।", "आशा आपके मन को आनन्द और शांति से भर दे।"),
        "es": ("Y el Dios de esperanza os llene de todo gozo y paz en el creer, para que abundéis en esperanza.", "Que la esperanza llene tu corazón de alegría y paz.")}),
    ("2 Corinthians", 9, 7, "charity,love", "happy", {
        "en": ("God loveth a cheerful giver.", "Giving with a happy heart is the best kind of giving."),
        "hi": ("परमेश्वर हर्ष से देनेवाले से प्रेम रखता है।", "ख़ुशी के मन से देना सबसे अच्छा देना है।"),
        "es": ("Dios ama al dador alegre.", "Dar con un corazón alegre es la mejor forma de dar.")}),
    ("2 Corinthians", 12, 9, "humility,courage", "sad,anxious", {
        "en": ("My grace is sufficient for thee: for my strength is made perfect in weakness.", "You are enough, and strength can grow even from weakness."),
        "hi": ("मेरा अनुग्रह तेरे लिये बहुत है; क्योंकि मेरी सामर्थ्य निर्बलता में सिद्ध होती है।", "आप पर्याप्त हैं, और कमज़ोरी से भी ताक़त उभर सकती है।"),
        "es": ("Bástate mi gracia; porque mi poder se perfecciona en la debilidad.", "Eres suficiente, y la fuerza puede nacer incluso de la debilidad.")}),
    ("Galatians", 6, 2, "compassion,love", "sad,happy", {
        "en": ("Bear ye one another's burdens, and so fulfil the law of Christ.", "Helping carry each other's heavy loads is an act of love."),
        "hi": ("तुम एक दूसरे के भार उठाओ, और इस प्रकार मसीह की व्यवस्था को पूरी करो।", "एक-दूसरे का भारी बोझ उठाने में मदद करना प्रेम का काम है।"),
        "es": ("Sobrellevad los unos las cargas de los otros, y cumplid así la ley de Cristo.", "Ayudarnos a llevar las cargas pesadas es un acto de amor.")}),
    ("Galatians", 6, 9, "patience,hope", "sad,anxious", {
        "en": ("And let us not be weary in well doing: for in due season we shall reap, if we faint not.", "Keep doing good; in time, the results will come."),
        "hi": ("हम भले काम करने में हियाव न छोड़ें, क्योंकि यदि हम ढीले न हों तो ठीक समय पर कटनी काटेंगे।", "भला करते रहो; समय आने पर परिणाम मिलेंगे।"),
        "es": ("No nos cansemos, pues, de hacer bien; porque a su tiempo segaremos, si no desmayamos.", "Sigue haciendo el bien; con el tiempo llegarán los frutos.")}),
    ("Philippians", 4, 7, "peace,faith", "anxious", {
        "en": ("And the peace of God, which passeth all understanding, shall keep your hearts and minds through Christ Jesus.", "A deep calm can guard your heart and thoughts."),
        "hi": ("तब परमेश्वर की शान्ति, जो समझ से बिलकुल परे है, तुम्हारे हृदय और तुम्हारे विचारों को सुरक्षित रखेगी।", "एक गहरी शांति आपके हृदय और विचारों की रक्षा कर सकती है।"),
        "es": ("Y la paz de Dios, que sobrepasa todo entendimiento, guardará vuestros corazones y vuestros pensamientos.", "Una calma profunda puede proteger tu corazón y tus pensamientos.")}),
    ("Philippians", 4, 8, "truth,wisdom", "happy,anxious", {
        "en": ("Whatsoever things are true, whatsoever things are honest, whatsoever things are just, whatsoever things are pure, whatsoever things are lovely, whatsoever things are of good report; think on these things.", "Fill your mind with what is true, good and lovely."),
        "hi": ("जो जो बातें सत्य हैं, जो जो बातें आदरणीय हैं, जो जो बातें उचित हैं, जो जो बातें पवित्र हैं, जो जो बातें सुहावनी हैं, जो जो बातें सुनाम हैं, इन्हीं पर ध्यान लगाया करो।", "अपने मन को सच्ची, अच्छी और सुंदर बातों से भरो।"),
        "es": ("Todo lo que es verdadero, todo lo honesto, todo lo justo, todo lo puro, todo lo amable, todo lo que es de buen nombre, en esto pensad.", "Llena tu mente de lo verdadero, lo bueno y lo hermoso.")}),
    ("Colossians", 3, 12, "compassion,humility", "happy,angry", {
        "en": ("Put on therefore, as the elect of God, holy and beloved, bowels of mercies, kindness, humbleness of mind, meekness, longsuffering.", "Wear kindness, humility and patience like clothes every day."),
        "hi": ("इसलिये परमेश्वर के चुने हुओं के समान, जो पवित्र और प्रिय हैं, तुम करुणा, कृपा, दीनता, नम्रता और सहनशीलता धारण करो।", "दया, विनम्रता और धैर्य को रोज़ कपड़ों की तरह पहनो।"),
        "es": ("Vestíos, pues, como escogidos de Dios, santos y amados, de entrañable misericordia, de benignidad, de humildad, de mansedumbre, de paciencia.", "Vístete cada día de bondad, humildad y paciencia.")}),
    ("Colossians", 3, 13, "forgiveness,patience", "angry,sad", {
        "en": ("Forbearing one another, and forgiving one another, if any man have a quarrel against any: even as Christ forgave you, so also do ye.", "Be patient with people and forgive them, as you have been forgiven."),
        "hi": ("एक दूसरे की सह लो, और यदि किसी को किसी पर दोष देने को कुछ हो, तो एक दूसरे के अपराध क्षमा करो; जैसे प्रभु ने तुम्हारे अपराध क्षमा किए, वैसे ही तुम भी करो।", "लोगों के साथ धैर्य रखो और उन्हें माफ़ करो, जैसे तुम्हें माफ़ किया गया है।"),
        "es": ("Soportándoos unos a otros, y perdonándoos unos a otros si alguno tuviere queja contra otro. De la manera que Cristo os perdonó, así también hacedlo vosotros.", "Ten paciencia con la gente y perdónala, como tú has sido perdonado.")}),
    ("1 Thessalonians", 5, 16, "gratitude,hope", "happy", {
        "en": ("Rejoice evermore.", "Look for reasons to be glad every day."),
        "hi": ("सदा आनन्दित रहो।", "हर दिन ख़ुश होने की वजहें ढूँढ़ो।"),
        "es": ("Estad siempre gozosos.", "Busca cada día motivos para alegrarte.")}),
    ("2 Timothy", 1, 7, "courage,self-control", "anxious", {
        "en": ("For God hath not given us the spirit of fear; but of power, and of love, and of a sound mind.", "You were made for strength, love and a calm mind, not for fear."),
        "hi": ("क्योंकि परमेश्वर ने हमें भय की नहीं, पर सामर्थ्य, प्रेम और संयम की आत्मा दी है।", "आपको डर के लिए नहीं, ताक़त, प्रेम और शांत मन के लिए बनाया गया है।"),
        "es": ("Porque no nos ha dado Dios espíritu de cobardía, sino de poder, de amor y de dominio propio.", "Fuiste hecho para la fuerza, el amor y la calma, no para el miedo.")}),
    ("Hebrews", 11, 1, "faith,hope", "anxious,happy", {
        "en": ("Now faith is the substance of things hoped for, the evidence of things not seen.", "Faith means trusting in good things even before you can see them."),
        "hi": ("अब विश्वास आशा की हुई वस्तुओं का निश्चय, और अनदेखी वस्तुओं का प्रमाण है।", "विश्वास का अर्थ है अच्छी बातों पर भरोसा करना, भले ही वे अभी दिखाई न दें।"),
        "es": ("Es, pues, la fe la certeza de lo que se espera, la convicción de lo que no se ve.", "Tener fe es confiar en lo bueno antes de verlo.")}),
    ("James", 1, 5, "wisdom,humility", "anxious", {
        "en": ("If any of you lack wisdom, let him ask of God, that giveth to all men liberally, and upbraideth not; and it shall be given him.", "It is okay to ask for wisdom when you are unsure."),
        "hi": ("पर यदि तुम में से किसी में बुद्धि की घटी हो, तो परमेश्वर से माँगे, जो बिना उलाहना दिये सब को उदारता से देता है; और उसको दी जाएगी।", "जब आप निश्चित न हों तो समझ माँगना ठीक है।"),
        "es": ("Y si alguno de vosotros tiene falta de sabiduría, pídala a Dios, el cual da a todos abundantemente y sin reproche, y le será dada.", "Está bien pedir sabiduría cuando no estás seguro.")}),
    ("1 Peter", 5, 7, "faith,peace", "anxious,sad", {
        "en": ("Casting all your care upon him; for he careth for you.", "Give your worries away; someone cares for you."),
        "hi": ("अपनी सारी चिन्ता उसी पर डाल दो, क्योंकि उसको तुम्हारा ध्यान है।", "अपनी चिंताएँ सौंप दो; कोई आपकी परवाह करता है।"),
        "es": ("Echando toda vuestra ansiedad sobre él, porque él tiene cuidado de vosotros.", "Entrega tus preocupaciones; alguien se preocupa por ti.")}),
    ("1 Peter", 4, 8, "love,forgiveness", "angry,happy", {
        "en": ("And above all things have fervent charity among yourselves: for charity shall cover the multitude of sins.", "Deep love helps us overlook many faults."),
        "hi": ("सबसे बढ़कर एक दूसरे से अधिक प्रेम रखो, क्योंकि प्रेम अनेक पापों को ढाँप देता है।", "गहरा प्रेम हमें अनेक कमियों को अनदेखा करना सिखाता है।"),
        "es": ("Y ante todo, tened entre vosotros ferviente amor; porque el amor cubrirá multitud de pecados.", "El amor profundo nos ayuda a pasar por alto muchas faltas.")}),
    ("1 John", 4, 7, "love", "happy,sad", {
        "en": ("Beloved, let us love one another: for love is of God; and every one that loveth is born of God, and knoweth God.", "Love is the heart of everything good."),
        "hi": ("हे प्रियो, हम आपस में प्रेम रखें; क्योंकि प्रेम परमेश्वर से है; और जो कोई प्रेम करता है, वह परमेश्वर से जन्मा है और परमेश्वर को जानता है।", "प्रेम हर अच्छी चीज़ का हृदय है।"),
        "es": ("Amados, amémonos unos a otros; porque el amor es de Dios. Todo aquel que ama, es nacido de Dios, y conoce a Dios.", "El amor es el corazón de todo lo bueno.")}),
    ("1 John", 4, 18, "love,courage", "anxious", {
        "en": ("There is no fear in love; but perfect love casteth out fear.", "Where there is real love, fear loses its power."),
        "hi": ("प्रेम में भय नहीं होता, वरन् सिद्ध प्रेम भय को दूर कर देता है।", "जहाँ सच्चा प्रेम है, वहाँ भय की ताक़त घट जाती है।"),
        "es": ("En el amor no hay temor, sino que el perfecto amor echa fuera el temor.", "Donde hay amor verdadero, el miedo pierde su fuerza.")}),
    ("Psalms", 19, 14, "humility,wisdom", "happy,anxious", {
        "en": ("Let the words of my mouth, and the meditation of my heart, be acceptable in thy sight, O LORD, my strength, and my redeemer.", "A good prayer is that our words and thoughts be kind and true."),
        "hi": ("हे यहोवा, मेरी चट्टान और मेरे उद्धारकर्ता, मेरे मुँह के वचन और मेरे हृदय का ध्यान तेरे सम्मुख ग्रहण योग्य हों।", "अच्छी प्रार्थना यही है कि हमारे शब्द और विचार दयालु और सच्चे हों।"),
        "es": ("Sean gratos los dichos de mi boca y la meditación de mi corazón delante de ti, oh Jehová, roca mía, y redentor mío.", "Una buena oración es que nuestras palabras y pensamientos sean amables y verdaderos.")}),
    ("Psalms", 62, 1, "faith,peace", "anxious,sad", {
        "en": ("Truly my soul waiteth upon God: from him cometh my salvation.", "Quiet trust brings help and calm."),
        "hi": ("मेरा मन परमेश्वर ही की ओर चुपचाप लगा रहता है; मेरा उद्धार उसी से होता है।", "शांत भरोसा सहायता और चैन लाता है।"),
        "es": ("En Dios solamente está acallada mi alma; de él viene mi salvación.", "La confianza tranquila trae ayuda y calma.")}),
    ("Psalms", 103, 2, "gratitude", "happy", {
        "en": ("Bless the LORD, O my soul, and forget not all his benefits.", "Remember the many good things you have received."),
        "hi": ("हे मेरे मन, यहोवा को धन्य कह, और उसके किसी उपकार को न भूल।", "जो अच्छी चीज़ें आपको मिली हैं, उन्हें याद रखिए।"),
        "es": ("Bendice, alma mía, a Jehová, y no olvides ninguno de sus beneficios.", "Recuerda las muchas cosas buenas que has recibido.")}),
    ("Proverbs", 14, 29, "patience,self-control", "angry", {
        "en": ("He that is slow to wrath is of great understanding: but he that is hasty of spirit exalteth folly.", "A patient person understands more than a hot-tempered one."),
        "hi": ("जो विलम्ब से क्रोध करनेवाला है, वह बड़ा समझवाला है, परन्तु जो उतावली करता है, वह मूर्खता को बढ़ाता है।", "धैर्यवान व्यक्ति गर्म मिज़ाज वाले से अधिक समझता है।"),
        "es": ("El que tarda en airarse es grande de entendimiento; mas el impaciente de espíritu enaltece la necedad.", "Una persona paciente entiende más que una de mal genio.")}),
    ("Proverbs", 27, 17, "wisdom,love", "happy", {
        "en": ("Iron sharpeneth iron; so a man sharpeneth the countenance of his friend.", "Good friends help each other grow sharper and better."),
        "hi": ("जैसे लोहा लोहे को चमका देता है, वैसे ही मनुष्य अपने मित्र के मुख को चमका देता है।", "अच्छे मित्र एक-दूसरे को बेहतर बनाते हैं।"),
        "es": ("Hierro con hierro se aguza; y así el hombre aguza el rostro de su amigo.", "Los buenos amigos se ayudan a crecer y a ser mejores.")}),
    ("Isaiah", 58, 10, "charity,compassion", "happy,sad", {
        "en": ("And if thou draw out thy soul to the hungry, and satisfy the afflicted soul; then shall thy light rise in obscurity.", "Caring for those in need brings light into your own life."),
        "hi": ("और यदि तू भूखे की ओर अपना मन लगाए और दीन-दुःखी को तृप्त करे, तो तेरा प्रकाश अंधकार में उदय होगा।", "ज़रूरतमंदों की देखभाल करने से आपके अपने जीवन में उजाला आता है।"),
        "es": ("Y si dieres tu pan al hambriento, y saciares al alma afligida, en las tinieblas nacerá tu luz.", "Cuidar de los necesitados trae luz a tu propia vida.")}),
    ("Matthew", 5, 16, "charity,wisdom", "happy", {
        "en": ("Let your light so shine before men, that they may see your good works.", "Let your good deeds shine so others feel hope."),
        "hi": ("उसी प्रकार तुम्हारा उजियाला मनुष्यों के सामने चमके कि वे तुम्हारे भले कामों को देखें।", "अपने अच्छे कामों को चमकने दो ताकि दूसरों को आशा मिले।"),
        "es": ("Así alumbre vuestra luz delante de los hombres, para que vean vuestras buenas obras.", "Deja que tus buenas obras brillen para que otros sientan esperanza.")}),
    ("Luke", 6, 37, "forgiveness,humility", "angry", {
        "en": ("Judge not, and ye shall not be judged: condemn not, and ye shall not be condemned: forgive, and ye shall be forgiven.", "Be slow to judge and quick to forgive."),
        "hi": ("दोष मत लगाओ, तो तुम पर भी दोष नहीं लगाया जाएगा; क्षमा करो, तो तुम्हें भी क्षमा किया जाएगा।", "दोष देने में धीमे और माफ़ करने में तत्पर रहो।"),
        "es": ("No juzguéis, y no seréis juzgados; no condenéis, y no seréis condenados; perdonad, y seréis perdonados.", "Sé lento para juzgar y rápido para perdonar.")}),
]
