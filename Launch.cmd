@echo off
setlocal EnableExtensions
cd /d "%~dp0"

if /i "%~1"=="" (
  echo.
  echo  Launch launcher   — demarre le launcher ^(npm run dev dans Launcher^)
  echo  Launch site       — demarre le site local ^(http://localhost:5174^)
  echo.
  echo  Sous PowerShell, depuis ce dossier :  .\Launch.cmd launcher
  echo  Ou toujours :  npm run "Launch launcher"   /   npm run "Launch site"
  echo.
  exit /b 1
)

if /i "%~1"=="launcher" (
  call npm run "Launch launcher"
  exit /b %ERRORLEVEL%
)

if /i "%~1"=="site" (
  call npm run "Launch site"
  exit /b %ERRORLEVEL%
)

echo Commande inconnue : %~1
echo Utilisez : Launch launcher   ou   Launch site
exit /b 1
