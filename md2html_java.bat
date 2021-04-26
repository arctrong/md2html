@echo off

set SUCCESS=

for /f "delims=" %%a in (md2html_java_list.txt) do call :GENERATE_ONE_HTML %* %%a

if not [%SUCCESS%]==[Y] pause
exit /b

:GENERATE_ONE_HTML
set SINGLE_SECCESS=N
echo.
echo Generating: %*
call java -jar %MD2HTML_JAVA_HOME%\md2html-0.1.1.jar %* && set SINGLE_SECCESS=Y
if not [%SINGLE_SECCESS%]==[Y] (
    set SUCCESS=N
) else (
    echo Done
    if not [%SUCCESS%]==[N] (
        set SUCCESS=Y
    )
)
exit /b
