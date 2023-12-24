@echo off

chcp 65001
set PYTHONUTF8=1

echo.

set INPUT_FILE=
set STATE=
for %%a in ('%~dp0_find_input_file.bat %IMPL% %*') do set INPUT_FILE="%%a"

echo Converting the following file to HTML: %INPUT_FILE%
echo.

for /f "delims=" %%a in ( '%~dp0..\_set_executable.bat %IMPL% %*' ) do set EXEC=%%a
call %EXEC% -fv "--include-css=%MD2HTML_HOME%\doc\layout\content.css" "--include-css=%MD2HTML_HOME%\doc\themes\light\content_theme.css" || pause 
