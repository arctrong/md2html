@echo off

setlocal

set ARGS=%*

call :RUN_TEST py
call :RUN_TEST java

exit /b

:RUN_TEST
echo.
echo ===== %1 implementation E2E tests =====
set IMPLEMENTATION=%1
python -m unittest discover %ARGS% --start-directory=%~dp0
exit /b
