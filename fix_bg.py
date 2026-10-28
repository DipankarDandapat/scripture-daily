path = r'c:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\MainActivity.kt'
with open(path, 'rb') as f:
    raw = f.read()
content = raw.decode('utf-8').replace('\r\n', '\n')

# 1. Home: remove the full-width amber gradient overlay Box
old1 = '        Box(Modifier.fillMaxWidth().height(260.dp).background(Brush.verticalGradient(listOf(Amber.copy(alpha = 0.07f), Color.Transparent))))\n'
new1 = ''

# 2. Saved: remove amber gradient from header Box, keep plain background
old2 = '        Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Amber.copy(alpha = 0.06f), Color.Transparent))).padding(horizontal = 20.dp, vertical = 20.dp)) {\n            Column {\n                Text(\"Saved Verses\"'
new2 = '        Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp, vertical = 20.dp)) {\n            Column {\n                Text(\"Saved Verses\"'

# 3. Explore: remove amber gradient from header Box, keep plain background
old3 = '        Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Amber.copy(alpha = 0.06f), Color.Transparent))).padding(horizontal = 20.dp, vertical = 20.dp)) {\n            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {'
new3 = '        Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp, vertical = 20.dp)) {\n            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {'

for old, new, name in [(old1, new1, 'Home gradient'), (old2, new2, 'Saved header'), (old3, new3, 'Explore header')]:
    if old not in content:
        print(f'ERROR: pattern not found for {name}')
        import sys; sys.exit(1)
    content = content.replace(old, new, 1)
    print(f'Fixed: {name}')

result = content.replace('\n', '\r\n')
with open(path, 'wb') as f:
    f.write(result.encode('utf-8'))
print('done')
