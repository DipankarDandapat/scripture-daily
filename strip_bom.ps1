$f = 'C:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\MainActivity.kt'
$bytes = [System.IO.File]::ReadAllBytes($f)
$start = 0
while ($start + 2 -lt $bytes.Length -and $bytes[$start] -eq 0xEF -and $bytes[$start+1] -eq 0xBB -and $bytes[$start+2] -eq 0xBF) {
    $start += 3
    Write-Output "Stripped one BOM at offset $($start-3)"
}
if ($start -gt 0) {
    $stripped = $bytes[$start..($bytes.Length-1)]
    [System.IO.File]::WriteAllBytes($f, $stripped)
    Write-Output "Saved. First 6 bytes now: $(($bytes[$start..($start+5)] | ForEach-Object { $_.ToString('X2') }) -join ' ')"
} else {
    Write-Output "No BOM found."
}
