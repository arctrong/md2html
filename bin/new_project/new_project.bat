@echo off
setlocal enabledelayedexpansion

set CANNOT_PROCEED=

if "%MD2HTML_HOME%"=="" (
    echo MD2HTML_HOME is not set
    exit /b 1
)

set "CHECKFILE=%MD2HTML_HOME%\bin\new_project\check_list.txt"
set "LISTFILE=%MD2HTML_HOME%\bin\new_project\copy_list.txt"

if not exist "%CHECKFILE%" (
    echo Check list not found: %CHECKFILE%
    exit /b 1
)

if not exist "%LISTFILE%" (
    echo Copy list not found: %LISTFILE%
    exit /b 1
)

for /f "usebackq eol=# delims=" %%i in ("%CHECKFILE%") do (
    call :check_file_or_dir "%%i"
)

if [%CANNOT_PROCEED%]==[Y] (
    echo Some problems found (see above^). Nothing has been done
    exit /b
)

for /f "usebackq eol=# tokens=1,2,3 delims=|" %%a in ("%LISTFILE%") do (
    set "src=%%a"
    set "dest=%%b"
    set "rec=%%c"
    if not "!src!"=="" (
        set "src=!src:/=\!"
        set "dest=!dest:/=\!"
        if "!rec:~0,1!"=="1" (
            xcopy "%MD2HTML_HOME%\!src!" "!dest!" /e /i /y
        ) else (
            xcopy "%MD2HTML_HOME%\!src!" "!dest!" /y
        )
    )
)

exit /b

:check_file_or_dir
if exist "%~1" (
    echo File or directory '%~1' already exists
    set CANNOT_PROCEED=Y
)
exit /b
