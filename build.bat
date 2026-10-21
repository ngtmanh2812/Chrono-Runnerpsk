@echo off
setlocal
if not exist out mkdir out
setlocal EnableDelayedExpansion
(for /f "delims=" %%f in ('dir /s /b src\*.java') do (
    set "source=%%f"
    set "source=!source:\=/!"
    echo "!source!"
)) > sources.txt
endlocal
javac -encoding UTF-8 -d out @sources.txt
if errorlevel 1 (
    echo BUILD FAILED
    exit /b 1
)
echo BUILD OK
