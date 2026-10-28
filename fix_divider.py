path = r'c:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\MainActivity.kt'
with open(path, 'rb') as f:
    raw = f.read()
lines = raw.decode('utf-8').splitlines(keepends=True)
for i, l in enumerate(lines):
    if 'HorizontalDivider' in l and 'BottomCenter' in l:
        print(f'Removing line {i+1}: {repr(l)}')
        lines.pop(i)
        break
with open(path, 'wb') as f:
    f.write(''.join(lines).encode('utf-8'))
print('done')
