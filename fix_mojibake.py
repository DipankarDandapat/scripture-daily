"""
Fix triple/double-encoded UTF-8 mojibake in all .kt files.

Strategy: scan the file for contiguous runs of chars that are all <= 255
(i.e., could be raw bytes), try to decode each run as UTF-8, and replace
if the result is valid and different. Repeat until stable.
"""
import os, glob, re

BASE = r'C:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily'

# Build cp1252 reverse map: unicode char -> byte value
CP1252 = {}
for _b in range(256):
    try:
        _c = bytes([_b]).decode('cp1252')
        CP1252[_c] = _b
    except Exception:
        CP1252[chr(_b)] = _b  # identity for undefined cp1252 bytes

def char_to_byte(c):
    if c in CP1252:
        return CP1252[c]
    if ord(c) < 256:
        return ord(c)
    return None  # above 255 and not in cp1252

def try_fix_run(run):
    """Try to decode a run of <= 255 chars as UTF-8 bytes. Return fixed string or None."""
    byte_vals = []
    for c in run:
        b = char_to_byte(c)
        if b is None:
            return None
        byte_vals.append(b)
    raw = bytes(byte_vals)
    try:
        decoded = raw.decode('utf-8')
        # Only accept if it actually changed something meaningful
        if decoded != run and len(decoded) < len(run):
            return decoded
        return None
    except UnicodeDecodeError:
        return None

# Match runs of chars that are all in the cp1252/latin-1 range (potential mojibake)
# A mojibake run starts with Ã (U+00C3) or Â (U+00C2) and continues with chars <= U+20AC
MOJIBAKE_RUN = re.compile(r'[\u00c2\u00c3][\u0080-\u20ac\u0152\u0153\u0160\u0161\u0178\u017d\u017e\u0192\u02c6\u02dc\u2013\u2014\u2018-\u201e\u2020\u2021\u2022\u2026\u2030\u2039\u203a\u20ac\u2122]+')

def fix_text(text):
    """Repeatedly fix mojibake runs until stable."""
    for _ in range(10):
        new_text = text
        # Find all potential mojibake runs and try to fix them
        result = []
        pos = 0
        changed = False
        for m in MOJIBAKE_RUN.finditer(text):
            start, end = m.start(), m.end()
            run = m.group()
            fixed = try_fix_run(run)
            if fixed is not None:
                result.append(text[pos:start])
                result.append(fixed)
                pos = end
                changed = True
        result.append(text[pos:])
        new_text = ''.join(result)
        if not changed or new_text == text:
            break
        text = new_text
    return text

kt_files = glob.glob(os.path.join(BASE, '**', '*.kt'), recursive=True)
print(f'Scanning {len(kt_files)} .kt files...')

fixed_count = 0
for fpath in kt_files:
    with open(fpath, 'rb') as f:
        raw = f.read()

    try:
        original = raw.decode('utf-8')
    except UnicodeDecodeError:
        print(f'  SKIP (not UTF-8): {os.path.basename(fpath)}')
        continue

    if '\u00c3' not in original and '\u00c2' not in original:
        print(f'  OK:    {os.path.basename(fpath)}')
        continue

    fixed = fix_text(original)

    if fixed != original:
        newline = '\r\n' if b'\r\n' in raw else '\n'
        with open(fpath, 'w', encoding='utf-8', newline=newline) as f:
            f.write(fixed)
        print(f'  FIXED: {os.path.basename(fpath)}')
        fixed_count += 1
    else:
        print(f'  NO CHANGE: {os.path.basename(fpath)}')

print(f'\nDone. {fixed_count} file(s) fixed.')
