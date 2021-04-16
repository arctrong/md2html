@echo off

set TITLE=
set FORCE=
set PARENT=%~1
for %%a in ("%PARENT%") do set PARENT=%%~dpa
for %%a in ("%PARENT:~0,-1%") do set PARENT=%%~nxa

set TITLE_1=%PARENT%
set TITLE_2=%~n1
set TITLE_3=%PARENT% %~n1

echo Converting the following file to HTML: %~1
echo.
echo Options:
echo.
echo y - Generate with empty title
echo 1 - Generate with empty title "%TITLE_1%"
echo 2 - Generate with empty title "%TITLE_2%"
echo 3 - Generate with empty title "%TITLE_3%"
echo t - Enter another title and then generate
echo f - Set forceful HTML file regeneration and get back to this choice
echo n - Cancel with no changes
echo.
set CHOICE=
:again
set /p CHOICE="Please make your choice (y): "
if "%CHOICE%"=="" goto continue
if "%CHOICE%"=="Y" goto continue
if "%CHOICE%"=="y" goto continue
if "%CHOICE%"=="1" (
    set TITLE=-t "%TITLE_1%"
    goto continue
)
if "%CHOICE%"=="2" (
    set TITLE=-t "%TITLE_2%"
    goto continue
)
if "%CHOICE%"=="3" (
    set TITLE=-t "%TITLE_3%"
    goto continue
)
if "%CHOICE%"=="t" goto custom_title
if "%CHOICE%"=="f" (
    set FORCE=-f
    set CHOICE=
    echo Forceful HTML file regeneration is set
    goto again
)
if "%CHOICE%"=="N" goto EOF
if "%CHOICE%"=="n" goto EOF
goto again

:custom_title
echo.
echo Please avoid quotes and other special characters,
echo they may cause problems.
set /p TITLE="Enter your title: "
if not ["%TITLE%"]==[] set TITLE=-t "%TITLE%"
goto continue

:continue
REM echo python %MD2HTML_PY_HOME%\md2html.py -v %FORCE% -i %1 %TITLE%
call python %MD2HTML_PY_HOME%\md2html.py -v %FORCE% -i %1 %TITLE%
echo. 
pause

:EOF
echo Bye
