@echo off
setlocal
cd /d "%~dp0"
if not exist out mkdir out
where javac >nul 2>nul
if errorlevel 1 (
  echo JDK not found. Install JDK 17 or newer and make sure javac is on PATH.
  pause
  exit /b 1
)
echo Compiling Inventory Management System...
javac -encoding UTF-8 -cp "library/*" -d out *.java
if errorlevel 1 (
  echo Build failed. Review the errors above.
  pause
  exit /b 1
)
echo Starting StockPilot...
java -cp "out;library/*" main
if errorlevel 1 pause
endlocal
