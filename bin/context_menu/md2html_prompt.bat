@echo off

for /f "delims=" %%a in ( '%~dp0..\_set_executable.bat %*' ) do set EXEC=%%a

echo.

set INPUT_FILE=
set STATE=
for %%a in (%*) do call :FIND_INPUT_FILE %%a

set TITLE=
set FORCE=
for %%a in ("%INPUT_FILE%") do set PARENT=%%~dpa
for %%a in ("%PARENT:~0,-1%") do set PARENT=%%~nxa
for %%a in ("%INPUT_FILE%") do set FILE_NAME=%%~na

set TITLE_1=%PARENT%
set TITLE_2=%FILE_NAME%
set TITLE_3=%TITLE_1% %TITLE_2%

echo Converting the following file to HTML: %INPUT_FILE%
echo.
echo Options:
echo.
echo y - Generate with default title (if not defined then empty)
echo 0 - Generate with empty title
echo 1 - Generate with title "%TITLE_1%"
echo 2 - Generate with title "%TITLE_2%"
echo 3 - Generate with title "%TITLE_3%"
echo t - Enter your own title and then generate
echo f - Set forceful HTML file regeneration and get back to this choice
echo q - Cancel with no changes
echo.
set CHOICE=
:again
set /p CHOICE="Please make your choice (y): "
if "%CHOICE%"=="" goto continue
if "%CHOICE%"=="Y" goto continue
if "%CHOICE%"=="y" goto continue
if "%CHOICE%"=="0" (
    set TITLE=--title=
    goto continue
)
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
if "%CHOICE%"=="Q" goto EOF
if "%CHOICE%"=="q" goto EOF
goto again

:custom_title
echo.
echo Please avoid quotes and other special characters,
echo they may cause problems.
set /p TITLE="Enter your title: "
if not ["%TITLE%"]==[] set TITLE=-t "%TITLE%"
goto continue

:FIND_INPUT_FILE
if [%STATE%]==[END] exit /b
if [%STATE%]==[-i] (
    set INPUT_FILE=%~1
    set STATE=END
    exit /b
)
if [%~1]==[-i] set STATE=-i
exit /b

:continue
call %EXEC% %FORCE% %TITLE% -v
echo.
pause

:EOF
echo Bye
