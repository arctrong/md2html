@echo off

set INPUT_FILE=
set STATE=
for %%a in (%*) do call :FIND_INPUT_FILE %%a

echo %INPUT_FILE%
exit /b

:FIND_INPUT_FILE
    if [%STATE%]==[END] exit /b
    if [%STATE%]==[-i] (
        set INPUT_FILE=%~1
        set STATE=END
        exit /b
    )
    if [%~1]==[-i] set STATE=-i
    exit /b
