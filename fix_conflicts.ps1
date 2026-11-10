$files = @(
  'C:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\data\Api.kt',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\data\LocalStore.kt',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\data\PaletteRefreshWorker.kt',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\data\VerseSyncWorker.kt',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\features\challenges\ChallengesScreen.kt',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\features\discover\DiscoverScreen.kt',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\features\explain\ExplainSheet.kt',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\features\home\ThemedVerseCard.kt',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\features\mood\MoodCard.kt',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\features\Overlay.kt',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\features\studio\StudioScreen.kt',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\features\tree\TreeLogic.kt',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\features\tree\TreeStore.kt',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\features\tree\WisdomTreeUi.kt',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\features\truths\UniversalTruthsScreen.kt',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\lockscreen\BootReceiver.kt',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\lockscreen\LockScreenVerse.kt',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\MainActivity.kt',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\notification\VerseNotifier.kt',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\ui\WTheme.kt',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\widget\ScriptureWidget.kt',
  'C:\Users\Dipankar\Downloads\scripture-daily-project\android\app\src\main\java\com\scripturedaily\widget\WidgetStudioScreen.kt'
)

foreach ($f in $files) {
  $lines = [System.IO.File]::ReadAllLines($f, [System.Text.Encoding]::UTF8)
  $result = [System.Collections.Generic.List[string]]::new()
  $state = 'normal'
  foreach ($line in $lines) {
    if ($line -match '^<<<<<<<') { $state = 'head' }
    elseif ($line -match '^=======' -and $state -eq 'head') { $state = 'theirs' }
    elseif ($line -match '^>>>>>>>' -and $state -eq 'theirs') { $state = 'normal' }
    elseif ($state -eq 'normal' -or $state -eq 'theirs') { $result.Add($line) }
  }
  [System.IO.File]::WriteAllLines($f, $result, [System.Text.Encoding]::UTF8)
  Write-Host "Fixed: $([System.IO.Path]::GetFileName($f))"
}
Write-Host "All done."
