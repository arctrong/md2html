@echo off

set SUCCESS=

for /f "delims=" %%a in (md2html_list.txt) do call :GENERATE_ONE_HTML %* %%a

if not [%SUCCESS%]==[Y] pause
exit /b

:GENERATE_ONE_HTML
set SINGLE_SECCESS=N
echo Generating: %*
call python %MD2HTML_PY_HOME%\md2html.py -v %* && set SINGLE_SECCESS=Y
if not [%SINGLE_SECCESS%]==[Y] (
    set SUCCESS=N
) else (
    echo Done
    if not [%SUCCESS%]==[N] (
        set SUCCESS=Y
    )
)
exit /b
