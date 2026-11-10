$f = 'C:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\MainActivity.kt'
$bytes = [System.IO.File]::ReadAllBytes($f)
$hex = ($bytes[0..5] | ForEach-Object { $_.ToString('X2') }) -join ' '
Write-Output "First 6 bytes: $hex"

# Strip UTF-8 BOM (EF BB BF) if present
if ($bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
    Write-Output "BOM found, stripping..."
    $stripped = $bytes[3..($bytes.Length-1)]
    [System.IO.File]::WriteAllBytes($f, $stripped)
    Write-Output "Done."
} else {
    Write-Output "No BOM. First chars:"
    Write-Output ([System.Text.Encoding]::UTF8.GetString($bytes[0..80]))
}
