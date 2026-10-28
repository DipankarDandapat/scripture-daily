path = r'c:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\MainActivity.kt'

with open(path, 'rb') as f:
    raw = f.read()

lines = raw.decode('utf-8').splitlines(keepends=True)

# Find the line with 'private sealed class HealthState'
# The missing '}' should be inserted just before it
eol = '\r\n' if b'\r\n' in raw else '\n'

for i, line in enumerate(lines):
    if 'private sealed class HealthState' in line:
        print(f'Inserting missing }} before line {i+1}')
        lines.insert(i, '}' + eol)
        break

result = ''.join(lines)
with open(path, 'wb') as f:
    f.write(result.encode('utf-8'))
print('done')
