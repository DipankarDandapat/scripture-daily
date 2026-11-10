$root = 'C:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java'
$files = Get-ChildItem -Path $root -Recurse -Filter '*.kt'
$fixed = 0
foreach ($file in $files) {
    $bytes = [System.IO.File]::ReadAllBytes($file.FullName)
    $start = 0
    while ($start + 2 -lt $bytes.Length -and $bytes[$start] -eq 0xEF -and $bytes[$start+1] -eq 0xBB -and $bytes[$start+2] -eq 0xBF) {
        $start += 3
    }
    if ($start -gt 0) {
        $stripped = $bytes[$start..($bytes.Length-1)]
        [System.IO.File]::WriteAllBytes($file.FullName, $stripped)
        Write-Output "Stripped BOM: $($file.Name)"
        $fixed++
    }
}
Write-Output "Done. $fixed file(s) fixed."
