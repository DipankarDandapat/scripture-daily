path = r'c:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\MainActivity.kt'
with open(path, 'rb') as f:
    raw = f.read()
content = raw.decode('utf-8').replace('\r\n', '\n')

old = '                .background(Color(0xFFFAF6F0))\n                .padding(horizontal = 20.dp, vertical = 20.dp)'
new = '                .background(MaterialTheme.colorScheme.background)\n                .padding(horizontal = 20.dp, vertical = 20.dp)'

if old not in content:
    print('ERROR: pattern not found')
    import sys; sys.exit(1)

result = content.replace(old, new, 1).replace('\n', '\r\n')
with open(path, 'wb') as f:
    f.write(result.encode('utf-8'))
print('done')
