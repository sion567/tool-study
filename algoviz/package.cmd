@echo off
rem ============================================================
rem  AlgoViz one-click packaging (Windows)
rem  Usage:
rem    package.cmd            fast build (skip tests)
rem    package.cmd test       run full test suite first
rem  Output: target\jpackage\AlgoViz\AlgoViz.exe
rem  Distribute = zip the whole target\jpackage\AlgoViz folder
rem  (bundled JRE included, target machine needs no Java)
rem ============================================================
setlocal
set JAVA_HOME=D:\sdk\Java\jdk-21.0.10
set PATH=%JAVA_HOME%\bin;%PATH%
set APP_VERSION=1.0.0

cd /d "%~dp0"

if "%1"=="test" (
    echo [1/3] Running full test suite...
    call mvn -q test || goto :fail
) else (
    echo [1/3] Skipping tests ^(run "package.cmd test" to include them^)
)

echo [2/3] Maven package: jar + dependency collection...
call mvn -q package -DskipTests || goto :fail

echo [3/3] jpackage: building self-contained app image...
if exist target\jpackage\AlgoViz rmdir /s /q target\jpackage\AlgoViz
jpackage --type app-image ^
  --name AlgoViz ^
  --app-version %APP_VERSION% ^
  --input target\deploy ^
  --main-jar algoviz-0.1.0-SNAPSHOT.jar ^
  --dest target\jpackage ^
  --java-options "-Dfile.encoding=UTF-8" || goto :fail

echo.
echo ============================================================
echo  BUILD OK ^> target\jpackage\AlgoViz\AlgoViz.exe
echo  Distribute = zip the whole target\jpackage\AlgoViz folder
echo  (bundled JRE, no Java needed on target machine)
echo ============================================================
exit /b 0

:fail
echo.
echo BUILD FAILED - see error output above
exit /b 1
