# WisdomOne — Brand Guidelines for Meta Ads

These rules keep the 25 launch ads, future iterations and the Play listing consistent. They match the approved Play
Store creatives (`play_store_creatives/README.md`) and the ad generator (`tools/marketing/make_meta_ads.py`,
`tools/marketing/wo_kit.py`).

---

## 1. Logo

The WisdomOne mark is a **golden lotus beneath a glowing sun** on a deep-plum rounded square (source:
`docs/assets/logo.png`).

| File | Use |
|---|---|
| `logo/wisdomone_logo_256.png` | Small UI, email signatures, documents |
| `logo/wisdomone_logo_512.png` | General digital use, ad creatives |
| *(no 1024 px file)* | The largest master in the repo is the 512 px Play Store icon (`android/app/src/main/res/ic_launcher-playstore.png`). Export a 1024 px+ version from the original design source before print or very large use — do not upscale the 512 px file. |
| `logo/wisdomone_logo_round_512.png` | Circular avatars — Facebook Page and Instagram profile pictures, where platforms crop to a circle |

**Lockup used in ads:** the round-cropped mark followed by the word "WisdomOne" in Avenir Next Bold. The gap between
mark and word is one quarter of the mark's height; the word is about 62 % of the mark's height. On dark backgrounds the
word is cream `#FFF8EC` or parchment; on light backgrounds it is deep plum `#1A0F2E`.

**Do**

- Keep clear space around the mark of at least 25 % of its width on every side.
- Use the mark at 48 px or larger on screen so the lotus stays legible.
- Use the round version wherever a platform crops to a circle.

**Don't**

- Stretch, rotate, recolour, outline or add effects (glows, bevels) to the mark.
- Place the mark on busy artwork without a calm area or veil behind it.
- Change the wordmark's spelling or case — always **WisdomOne** (one word, capital W and O).
- Combine the mark with religious symbols, other logos or badges that imply endorsement.

---

## 2. Colour palette

| Name | Hex | RGB | Typical use |
|---|---|---|---|
| Plum | `#2D1B4E` | 45, 27, 78 | Primary brand colour, dark backgrounds |
| Deep plum | `#1A0F2E` | 26, 15, 46 | Headlines on light backgrounds, darkest backgrounds |
| Purple | `#4B2E83` | 75, 46, 131 | Accents, buttons, "With WisdomOne" labels |
| Lavender | `#C4B0E8` | 196, 176, 232 | Light backgrounds, secondary text on dark |
| Gold | `#D4A574` | 212, 165, 116 | Accents, icons, ticks |
| Bright gold | `#EEC478` | 238, 196, 120 | Highlights and halos on dark backgrounds |
| Parchment | `#F8F2E9` | 248, 242, 233 | Light backgrounds, text on dark |
| Ink | `#1E1530` | 30, 21, 48 | Body text on light backgrounds |

**Palette variants used by the ads** (gradients built from the colours above plus supporting tints, defined in
`wo_kit.py`): `dawn`, `lavender`, `sand`, `sage`, `ocean` (light) · `plum`, `night`, `midnight` (dark) · `sunset`
(warm gradient). Use a variant as a whole — background, accent and text colours together — rather than mixing.

**Contrast:** headline and body text must meet WCAG AA contrast against their background (4.5:1 for body text, 3:1 for
large headlines). On artwork backgrounds, use the dark veil / scrim the generator applies.

---

## 3. Typography

| Role | Typeface | Notes |
|---|---|---|
| Headlines | **Palatino Bold Italic** | The serif italic of the approved "Make it Yours" settings design. Default for all headlines. |
| Body, labels, buttons, wordmark | **Avenir Next** (Regular, Medium, Demi Bold, Bold) | Clean geometric sans for everything that is not a headline. |
| Accent — cinematic headlines | Baskerville Italic | Only in the `cinematic` style (full-bleed artwork). |
| Accent — large verse quotations | Didot (Regular / Bold / Italic) | Only in type-first styles (`typography`, `typography_night`) and the dark showcase (`split_dark`). |

**Open-licence alternatives** for teams without macOS (choose one per role and use it consistently):

| Role | Alternative |
|---|---|
| Headlines (for Palatino Bold Italic) | Playfair Display Italic (bold weight) or Lora Italic (bold weight) |
| Body (for Avenir Next) | Nunito Sans or Inter |

Rules: sentence case or title case as in the spec — never all caps for headlines; at most two typefaces per creative
(plus the accent face where the style calls for it); keep body text at a size readable on a phone in Feed (the
generator uses roughly 3–3.5 % of the image width).

See `fonts/README.md` for licensing.

---

## 4. Phone mockups

- **Device:** a modern thin-bezel Android phone with a punch-hole camera — never an iPhone or any recognisable
  third-party brand.
- **Screens:** only real app screenshots (captured from the running app) or wallpapers rendered by the app's own
  `LockScreenVerse` / live-wallpaper code. No mock UI, no invented features.
- **Status bar / lock clock:** neutral demo values (e.g. 9:41) — no personal notifications, names or messages.
- **Angles:** upright or a gentle tilt (up to about ±9°). No extreme perspective that distorts the screen text.
- **Shadows:** soft, single-direction drop shadow; no reflections.
- **Text inside phones** must stay legible at Feed size; if it is not, enlarge a crop (as the `verse_card` and
  `tree_focus` styles do) rather than shrinking the message.

---

## 5. Imagery

- **The lock screen leads.** The lock-screen verse is WisdomOne's most distinctive feature; every ad shows a lock
  screen or a verse wallpaper prominently, unless the composition is a Home / Settings showcase that still includes one.
- **Real app content only.** Backgrounds come from the app's bundled artwork, the approved Settings landscape, or
  code-generated gradients.
- **No people.** No faces, hands, bodies or silhouettes — this also removes any risk of "before / after" claims about
  people or of implying who the viewer is.
- **Verses quoted exactly** from the app catalogue (King James Version for English), always with the reference
  (e.g. "Psalms 46:10"). Never paraphrase or invent a verse, and never put words in scripture's mouth.
- **No religious symbols as decoration** beyond what the app's own artwork contains; no imagery from other traditions
  in Bible-verse ads.
- **Safe zones for 9:16:** keep key text out of the top 13 % and bottom 22 % of the frame (Stories / Reels interface).
- **Text in image:** keep it short — eyebrow, headline, one supporting line. The detail goes in the primary text.

---

## 6. Tone of voice

| WisdomOne is… | …not |
|---|---|
| Calm, warm, gentle | Loud, urgent, pushy ("Hurry!", "Last chance") |
| Clear and concrete ("a verse on your lock screen") | Vague or exaggerated ("life-changing", "miracle") |
| Inviting ("Find a moment of peace") | Presumptuous about the reader ("Your faith needs this") |
| Respectful of scripture | Casual or jokey about sacred text |
| Honest ("free", "refreshes automatically") | Claims of rankings, results or popularity |

Writing rules: short sentences; one idea per sentence; British spelling, as in the launch copy ("colour",
"favourite", "personalised") — slugs such as `personalize_everyday` are file names, not copy; typographic apostrophes
and dashes.

---

## 7. Do's and don'ts (summary)

| Do | Don't |
|---|---|
| Show the real lock screen and real app screens | Show features the app does not have |
| Quote verses exactly, with reference | Paraphrase scripture or omit the reference |
| Describe the app ("a daily Bible verse app") | Address the reader's religion or state of mind |
| Say "free" (it is) | Mention prices, discounts, "premium tier" or subscriptions |
| Keep one clear message per ad | Crowd an ad with every feature (except the deliberate `feature_list` / `product_grid` styles) |
| Use the official Google Play badge if a badge is used | Draw a look-alike Play badge or imply Google endorsement |
| Keep palettes and typefaces from this guide | Introduce new colours or fonts per ad |

---

## 8. Meta ad policy notes

These notes summarise points relevant to WisdomOne; they are not a substitute for Meta's current Advertising Standards,
which should be checked before launch and when policies change.

- **Personal attributes:** ads must not assert or imply a person's religion or beliefs, health (including mental
  health), or other personal attributes. "Are you Christian?" or "Feeling anxious?" are not acceptable; "A daily Bible
  verse, beautifully presented" is. See `../00_CAMPAIGN_STRATEGY/audience_strategy.md` §1.
- **Targeting:** do not target or infer religion; religious detailed-targeting options were removed by Meta.
- **Before / after:** the `before_after` style compares two lock screens only. Never show before / after images of
  people.
- **Misleading claims:** no claims of results, rankings or popularity; every feature shown must exist in the shipped app.
- **Landing experience:** the Play listing must match the ad (name, icon, screenshots, "free") and be live.
- **Third-party brands:** no Google, Android, Facebook or Instagram logos or UI imitations beyond what platform brand
  guidelines allow.
- **Text in images:** Meta no longer rejects ads for image-text percentage, but heavy text can reduce delivery — keep
  in-image text short.
- **Music in videos:** the five videos use original music generated by `make_meta_videos.py`; do not replace it with
  unlicensed tracks.
