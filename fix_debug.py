import sys

# Build full cp1252 reverse map: unicode char -> byte value
cp = {}
for b in range(256):
    try:
        c = bytes([b]).decode('cp1252')
        cp[c] = b
    except Exception:
        pass  # skip undefined cp1252 bytes (0x81,0x8D,0x8F,0x90,0x9D)

out = []
out.append("cp1252 special chars:")
for c in ['\u0192', '\u201a', '\u20ac', '\u0161', '\u009d']:
    out.append("  U+%04X -> byte 0x%02X" % (ord(c), cp.get(c, -1)))

def enc(s):
    result = []
    for c in s:
        if c in cp:
            result.append(cp[c])
        elif ord(c) < 256:
            result.append(ord(c))
        else:
            return None  # char > 255 and not in cp1252 map
    return bytes(result)

# Test on the actual chunk
chunk_hex = 'c383c692c382c2a2c383c2a2c3a2e2809ac2acc382c29d'
s = bytes.fromhex(chunk_hex).decode('utf-8')
out.append("\nchunk: " + repr(s))

b1 = enc(s)
out.append("b1: " + (b1.hex() if b1 else "None"))
if b1:
    try:
        s1 = b1.decode('utf-8')
        out.append("r1: " + repr(s1))
        b2 = enc(s1)
        out.append("b2: " + (b2.hex() if b2 else "None"))
        if b2:
            try:
                s2 = b2.decode('utf-8')
                out.append("r2: " + repr(s2))
                b3 = enc(s2)
                out.append("b3: " + (b3.hex() if b3 else "None"))
                if b3:
                    try:
                        s3 = b3.decode('utf-8')
                        out.append("r3: " + repr(s3))
                    except Exception as e:
                        out.append("r3 err: " + str(e))
            except Exception as e:
                out.append("r2 err: " + str(e))
    except Exception as e:
        out.append("r1 err: " + str(e))

# Also test the icon line
icon_hex = 'c383c692c382c2a2c383e280a6c3a2e282acc593c383c2a2c3a2e2809ac2acc382c2a2'
s = bytes.fromhex(icon_hex).decode('utf-8')
out.append("\nicon chunk: " + repr(s))
b1 = enc(s)
if b1:
    try:
        s1 = b1.decode('utf-8')
        out.append("icon r1: " + repr(s1))
        b2 = enc(s1)
        if b2:
            try:
                s2 = b2.decode('utf-8')
                out.append("icon r2: " + repr(s2))
                b3 = enc(s2)
                if b3:
                    try:
                        s3 = b3.decode('utf-8')
                        out.append("icon r3: " + repr(s3))
                    except Exception as e:
                        out.append("icon r3 err: " + str(e))
            except Exception as e:
                out.append("icon r2 err: " + str(e))
    except Exception as e:
        out.append("icon r1 err: " + str(e))

with open(r'C:\Users\Dipankar\Downloads\scripture-daily-project\debug_out.txt', 'w', encoding='ascii', errors='backslashreplace') as f:
    f.write('\n'.join(out))

print("done")
