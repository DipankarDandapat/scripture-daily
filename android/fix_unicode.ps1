$file = 'C:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\MainActivity.kt'
$content = [System.IO.File]::ReadAllText($file, [System.Text.Encoding]::UTF8)

# Fix corrupted unicode: Ã‚Â· should be · and Ãƒâ€" should be ×
$content = $content.Replace("` Ã‚Â· `${item.readCount}Ãƒâ€`"", " · `${item.readCount}×")
$content = $content.Replace("Ã‚Â· `${item.readCount}Ãƒâ€`"", "· `${item.readCount}×")

# Also fix any remaining corrupted sequences in this block
$old = 'val countLabel = if (item.readCount > 1) " Ã‚Â· ${item.readCount}Ãƒâ€"" else ""'
$new = 'val countLabel = if (item.readCount > 1) " \u00b7 ${item.readCount}\u00d7" else ""'
$content = $content.Replace($old, $new)

[System.IO.File]::WriteAllText($file, $content, [System.Text.UTF8Encoding]::new($false))
Write-Host "Done"
Write-Host "Contains countLabel: $($content.Contains('countLabel'))"
