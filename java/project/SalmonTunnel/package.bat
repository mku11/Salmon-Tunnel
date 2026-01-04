@echo ON
set CURRDIR=%CD%

set VERSION=1.0.0

:: Salmon Tunnel
set OUTPUT_ROOT=..\..\..\output
set OUTPUT_DIR=%OUTPUT_ROOT%\java
set SALMON_TUNNEL_JAR=.\build\libs\SalmonTunnel-%VERSION%.jar
set SALMON_LIBS_DIR=.\libs
set SALMON_SCRIPTS_DIR=.\scripts

set PACKAGES_DIR=app_package
set SALMON_TUNNEL=salmon-tunnel
set SALMON_TUNNEL_PACKAGE_NAME=%SALMON_TUNNEL%

powershell mkdir -ErrorAction SilentlyContinue %PACKAGES_DIR%
del /S /Q .\%PACKAGES_DIR%\*

powershell mkdir -ErrorAction SilentlyContinue %PACKAGES_DIR%\%SALMON_TUNNEL_PACKAGE_NAME%
del /S /Q .\%PACKAGES_DIR%\%SALMON_TUNNEL_PACKAGE_NAME%\*

robocopy /E %SALMON_LIBS_DIR%\ %PACKAGES_DIR%\%SALMON_TUNNEL_PACKAGE_NAME%\salmon\
copy %SALMON_TUNNEL_JAR% %PACKAGES_DIR%\%SALMON_TUNNEL_PACKAGE_NAME%\salmon\

copy %SALMON_SCRIPTS_DIR%\* %PACKAGES_DIR%\%SALMON_TUNNEL_PACKAGE_NAME%\
copy README.txt %PACKAGES_DIR%\%SALMON_TUNNEL_PACKAGE_NAME%\

cd %PACKAGES_DIR%\%SALMON_TUNNEL_PACKAGE_NAME%
powershell -command Compress-Archive -Force -DestinationPath ..\%SALMON_TUNNEL_PACKAGE_NAME%.zip *
cd ..\..\
powershell mkdir -ErrorAction SilentlyContinue %OUTPUT_DIR%
copy /Y %PACKAGES_DIR%\%SALMON_TUNNEL_PACKAGE_NAME%.zip %OUTPUT_DIR%

cd %CURRDIR%