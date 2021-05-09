@echo off
setlocal

for /f "delims=" %%a in ('%~dp0_set_executable.bat %*') do set EXEC=%%a

set SUCCESS=
for /f "delims=" %%a in (md2html_list.txt) do call :GENERATE_ONE_HTML -v %%a
exit /b

:GENERATE_ONE_HTML
set SINGLE_SECCESS=N
echo Generating: %*
call %EXEC% %* && set SINGLE_SECCESS=Y
if not [%SINGLE_SECCESS%]==[Y] (
    set SUCCESS=N
    exit /b
)
echo Done
REM if not [%SUCCESS%]==[N] set SUCCESS=Y
if [%SUCCESS%]==[N] exit /b 1
exit /b
