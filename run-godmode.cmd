@echo off
setlocal
pushd "%~dp0"
if errorlevel 1 exit /b 1
call build.bat
if errorlevel 1 (
    popd
    pause
    exit /b 1
)
echo GOD MODE ON - Both players take no combat damage.
echo Close this game and use run.bat to play normally.
java -Dchronorunner.godMode=true -cp out chronorunner.Main
set "gameExit=%errorlevel%"
popd
if not "%gameExit%"=="0" pause
exit /b %gameExit%
