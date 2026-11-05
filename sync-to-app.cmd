@echo off
rem Copies this project's changes into its installed app (%LOCALAPPDATA%\Programs\<folder name>\resources\app).
rem Usage: sync-to-app          copy changes (backs up the installed app first)
rem        sync-to-app /dry     only list what would be copied
setlocal
set "SRC=%~dp0"
set "SRC=%SRC:~0,-1%"
for %%I in ("%SRC%") do set "NAME=%%~nxI"
set "APP=%LOCALAPPDATA%\Programs\%NAME%\resources\app"
set "BAK=%LOCALAPPDATA%\Programs\%NAME%\resources\app-before-sync"

if not exist "%SRC%\package.json" (echo No package.json in "%SRC%". Nothing was changed. & exit /b 1)
if not exist "%APP%\package.json" (echo Can't find the installed %NAME% app at "%APP%". Nothing was changed. & exit /b 1)

set "DRY="
if /i "%~1"=="/dry" set "DRY=/L"

if not defined DRY (
  echo Backing up the installed app to "%BAK%" ...
  robocopy "%APP%" "%BAK%" /MIR /R:1 /W:1 /NFL /NDL /NJH /NJS /NP >nul
  if errorlevel 8 (echo Backup failed. Nothing was changed. & exit /b 1)
)

rem Never deletes anything from the app; only copies files that are new or newer here.
robocopy "%SRC%" "%APP%" /E /XO %DRY% /XD .gg .git mobile /XF *.bak sync-to-app.cmd /R:1 /W:1 /NDL /NJH /NP
if errorlevel 8 (echo Copy failed. Restore from "%BAK%" if the app misbehaves. & exit /b 1)

if defined DRY (echo Dry run: the files listed above would be copied.) else (echo Done. Restart %NAME% to load the changes.)
exit /b 0
