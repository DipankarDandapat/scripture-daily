# WisdomOne — Fonts

## Fonts used in the ad creatives

The images and videos are rendered by `tools/marketing/make_meta_ads.py` and `tools/marketing/make_meta_videos.py`
using these macOS system fonts (paths as configured in `tools/marketing/wo_kit.py`):

| Typeface | macOS file | Used for |
|---|---|---|
| Palatino (Bold Italic) | `/System/Library/Fonts/Palatino.ttc` | Headlines — the serif italic of the approved "Make it Yours" design |
| Avenir Next (Regular, Medium, Demi Bold, Bold) | `/System/Library/Fonts/Avenir Next.ttc` | Body text, labels, buttons, the "WisdomOne" wordmark, lock-screen clock |
| Baskerville (Italic) | `/System/Library/Fonts/Supplemental/Baskerville.ttc` | Headlines in the `cinematic` style only |
| Didot (Regular, Bold, Italic) | `/System/Library/Fonts/Supplemental/Didot.ttc` | Large verse quotations and headlines in `typography`, `typography_night` and `split_dark` |

## Licence note

- These fonts ship with macOS and are licensed to the Mac they are installed on. They are used **only to render the
  campaign images and videos**; the rendered pixels are what is published.
- **The font files are not included in this folder and must not be redistributed** — do not copy them into the
  repository, share them with agencies, or embed them in web pages or apps.
- Anyone regenerating the creatives needs a Mac with these system fonts installed (they are present on a standard macOS
  installation; Baskerville and Didot are in the `Supplemental` font folder).

## Open-licence alternatives

For designers or agencies without macOS, or for any use where the font file itself must be shipped (web pages,
landing pages, editable templates), use these SIL Open Font Licence families from Google Fonts:

| Role | Alternative | Link |
|---|---|---|
| Headlines (Palatino Bold Italic) | Playfair Display — Bold Italic | https://fonts.google.com/specimen/Playfair+Display |
| Headlines (Palatino Bold Italic) | Lora — Bold Italic | https://fonts.google.com/specimen/Lora |
| Body (Avenir Next) | Nunito Sans | https://fonts.google.com/specimen/Nunito+Sans |
| Body (Avenir Next) | Inter | https://fonts.google.com/specimen/Inter |

Pick **one** headline alternative and **one** body alternative and use them consistently across a project. Do not mix
the macOS originals and the alternatives within one creative.

Check the licence shown on each Google Fonts page before use; it should be confirmed again if fonts are bundled into a
product.
