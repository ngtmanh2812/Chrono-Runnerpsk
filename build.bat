@echo off
setlocal
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
if errorlevel 1 (
    echo BUILD FAILED
    exit /b 1
)
echo BUILD OK
