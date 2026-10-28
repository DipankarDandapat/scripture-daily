path = r'c:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\MainActivity.kt'

with open(path, 'rb') as f:
    raw = f.read()

lines = raw.decode('utf-8').splitlines(keepends=True)

# Print lines 425-435 for diagnosis
for i in range(424, 435):
    print(f'{i+1}: {repr(lines[i])}')

# Find the pattern: a line that is just '    }\r\n' immediately followed
# (after optional blank lines) by the State comment line
# The stray brace is the SECOND consecutive closing brace after apiOffline = true
for i in range(len(lines)):
    if lines[i].strip() == '}' and i > 0 and lines[i-1].strip() == '}':
        # Check context: previous non-blank before that should be 'apiOffline = true'
        j = i - 2
        while j >= 0 and lines[j].strip() == '':
            j -= 1
        if j >= 0 and 'apiOffline = true' in lines[j]:
            print(f'\nRemoving stray }} at line {i+1}: {repr(lines[i])}')
            lines.pop(i)
            break

result = ''.join(lines)
with open(path, 'wb') as f:
    f.write(result.encode('utf-8'))
print('done')
