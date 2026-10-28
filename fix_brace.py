path = r'c:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\MainActivity.kt'

with open(path, 'rb') as f:
    raw = f.read()

lines = raw.decode('utf-8').splitlines(keepends=True)

# Find the stray '    }' that immediately follows the closing '    }' of LaunchedEffect
# Pattern: line is '    }\r\n' and next non-empty line starts with '    // ' (State comment)
for i, line in enumerate(lines):
    stripped = line.strip()
    if stripped == '}' and i > 0:
        prev = lines[i-1].strip()
        if prev == '}':
            # Check next non-blank line is the State comment
            j = i + 1
            while j < len(lines) and lines[j].strip() == '':
                j += 1
            if j < len(lines) and 'State' in lines[j]:
                print(f'Removing stray brace at line {i+1}: {repr(line)}')
                lines.pop(i)
                break

result = ''.join(lines)
with open(path, 'wb') as f:
    f.write(result.encode('utf-8'))
print('done')
