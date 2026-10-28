path = r'c:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\MainActivity.kt'

with open(path, 'rb') as f:
    raw = f.read()

lines = raw.decode('utf-8').splitlines(keepends=True)

# Find start: line with 'var healthState by remember'
start = next(i for i, l in enumerate(lines) if 'var healthState by remember' in l)

# Find end: line with 'ApiHealthScreen' + 'return' block — scan forward for the closing '    }'
# The block ends after the 'return' line and its closing brace
end = start
while end < len(lines):
    if 'ApiHealthScreen' in lines[end]:
        # skip past 'return' and closing '    }'
        end += 1  # 'return'
        end += 1  # '    }'
        break
    end += 1

print(f'Replacing lines {start+1} to {end} (0-indexed {start} to {end-1})')
for i in range(start, end):
    print(f'  {start+1+(i-start)}: {lines[i].rstrip()}')

eol = '\r\n' if b'\r\n' in raw else '\n'

replacement = (
    f'    var apiOffline by remember {{ mutableStateOf(false) }}{eol}'
    f'{eol}'
    f'    LaunchedEffect(Unit) {{{eol}'
    f'        repeat(3) {{ attempt ->{eol}'
    f'            try {{{eol}'
    f'                val resp = ApiClient.api.health(){eol}'
    f'                if (resp.isSuccessful) {{ apiOffline = false; return@LaunchedEffect }}{eol}'
    f'            }} catch (_: Exception) {{}}{eol}'
    f'            if (attempt < 2) kotlinx.coroutines.delay(2000L){eol}'
    f'        }}{eol}'
    f'        apiOffline = true{eol}'
    f'    }}{eol}'
)

new_lines = lines[:start] + [replacement] + lines[end:]
result = ''.join(new_lines)

with open(path, 'wb') as f:
    f.write(result.encode('utf-8'))

print('done')
