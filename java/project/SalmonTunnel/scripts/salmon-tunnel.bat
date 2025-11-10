@echo off

SET CURRDIR=%~dp0
set SALMON_CLASS_PATH="%CURRDIR%/*;%CURRDIR%/libs/*;%CURRDIR%/salmon/*"
set MAIN_CLASS=com.mku.salmon.tunnel.main.Main
java -Djava.library.path="." -cp %SALMON_CLASS_PATH% %MAIN_CLASS% %*