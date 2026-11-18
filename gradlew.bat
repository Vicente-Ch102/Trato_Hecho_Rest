@echo off
setlocal
set GRADLE_VERSION=8.14.7
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /b %ERRORLEVEL%
)
if "%GRADLE_USER_HOME%"=="" set GRADLE_USER_HOME=%USERPROFILE%\.gradle
set DIST_DIR=%GRADLE_USER_HOME%\trato-hecho-gradle\gradle-%GRADLE_VERSION%
if not exist "%DIST_DIR%\bin\gradle.bat" (
  if not exist "%GRADLE_USER_HOME%\trato-hecho-gradle" mkdir "%GRADLE_USER_HOME%\trato-hecho-gradle"
  set ZIP=%GRADLE_USER_HOME%\trato-hecho-gradle\gradle-%GRADLE_VERSION%-bin.zip
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%GRADLE_USER_HOME%\trato-hecho-gradle\gradle-%GRADLE_VERSION%-bin.zip'; Expand-Archive -Force '%GRADLE_USER_HOME%\trato-hecho-gradle\gradle-%GRADLE_VERSION%-bin.zip' '%GRADLE_USER_HOME%\trato-hecho-gradle'"
  if not exist "%DIST_DIR%\bin\gradle.bat" exit /b 1
)
call "%DIST_DIR%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
