@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "MAVEN_VERSION=3.9.9"
set "MAVEN_SHA256=4ec3f26fb1a692473aea0235c300bd20f0f9fe741947c82c1234cefd76ac3a3c"
set "BASE=%~dp0"
set "DIST=%BASE%.mvn\dist\apache-maven-%MAVEN_VERSION%"
set "ZIP=%BASE%.mvn\dist\apache-maven-%MAVEN_VERSION%-bin.zip"
set "MARKER=%DIST%\.petlink-maven-sha256"

if exist "%DIST%\bin\mvn.cmd" if exist "%MARKER%" (
  set /p INSTALLED_SHA=<"%MARKER%"
  if /I "!INSTALLED_SHA!"=="%MAVEN_SHA256%" goto run
)

where powershell >nul 2>nul || (echo PowerShell is required for first Maven bootstrap.& exit /b 1)
if not exist "%BASE%.mvn\dist" mkdir "%BASE%.mvn\dist"
if exist "%DIST%" rmdir /s /q "%DIST%"
if exist "%ZIP%" del /q "%ZIP%"

echo Downloading Apache Maven %MAVEN_VERSION% ...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$u='https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip'; Invoke-WebRequest -UseBasicParsing -Uri $u -OutFile '%ZIP%'; $sha=[System.Security.Cryptography.SHA256]::Create(); $stream=[System.IO.File]::OpenRead('%ZIP%'); try { $h=([System.BitConverter]::ToString($sha.ComputeHash($stream))).Replace('-', '').ToLowerInvariant() } finally { $stream.Dispose(); $sha.Dispose() }; if ($h -ne '%MAVEN_SHA256%') { Remove-Item -LiteralPath '%ZIP%' -Force -ErrorAction SilentlyContinue; throw ('Maven SHA-256 mismatch. expected=%MAVEN_SHA256% actual=' + $h) }; Expand-Archive -Path '%ZIP%' -DestinationPath '%BASE%.mvn\dist' -Force" || exit /b 1
>"%MARKER%" echo %MAVEN_SHA256%

:run
call "%DIST%\bin\mvn.cmd" %*
