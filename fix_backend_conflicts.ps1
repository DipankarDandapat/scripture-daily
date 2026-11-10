$files = @(
  'C:\Users\Dipankar\Downloads\scripture-daily-project\backend\.env',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\backend\alembic\versions\0002_scripture_ids_multi.py',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\backend\alembic\versions\0003_wisdomone_features.py',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\backend\alembic\versions\0004_wisdomone_v2_enhancements.py',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\backend\app\api\v1\features.py',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\backend\app\api\v1\routes.py',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\backend\app\api\v1\studio.py',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\backend\app\core\config.py',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\backend\app\main.py',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\backend\app\models\entities.py',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\backend\app\schemas\api.py',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\backend\app\services\tree.py',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\backend\requirements.txt',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\backend\tests\test_api.py'
)

foreach ($f in $files) {
  if (-not (Test-Path $f)) { Write-Host "SKIP (not found): $f"; continue }
  $bytes = [System.IO.File]::ReadAllBytes($f)
  # Strip BOM if present
  $start = 0
  while ($start + 2 -lt $bytes.Length -and $bytes[$start] -eq 0xEF -and $bytes[$start+1] -eq 0xBB -and $bytes[$start+2] -eq 0xBF) { $start += 3 }
  $content = [System.Text.Encoding]::UTF8.GetString($bytes[$start..($bytes.Length-1)])

  if ($content -notmatch '<<<<<<< HEAD') { Write-Host "CLEAN: $([System.IO.Path]::GetFileName($f))"; continue }

  $lines = $content -split "`r?`n"
  $result = [System.Collections.Generic.List[string]]::new()
  $state = 'normal'
  foreach ($line in $lines) {
    if ($line -match '^<<<<<<<') { $state = 'head' }
    elseif ($line -match '^=======' -and $state -eq 'head') { $state = 'theirs' }
    elseif ($line -match '^>>>>>>>' -and $state -eq 'theirs') { $state = 'normal' }
    elseif ($state -eq 'normal' -or $state -eq 'theirs') { $result.Add($line) }
  }
  [System.IO.File]::WriteAllText($f, ($result -join "`n"), [System.Text.Encoding]::UTF8)
  Write-Host "Fixed: $([System.IO.Path]::GetFileName($f))"
}
Write-Host "Done."
