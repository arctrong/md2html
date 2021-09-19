@echo off
setlocal

for /f "delims=" %%a in ('%~dp0_set_executable.bat %*') do set EXEC=%%a

call %EXEC% --argument-file=md2html_args.json || exit /b 1
