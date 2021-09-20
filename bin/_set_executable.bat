@echo off

set PYTHON_EXEC=python "%MD2HTML_HOME%\python\md2html.py"
set JAVA_EXEC=java -jar "%MD2HTML_HOME%\java\target\md2html-0.1.3-bin.jar"
set DEFAULT_EXEC=%PYTHON_EXEC%

set EXEC_WAS_SET=
set REMAINED_ARGS=
for %%a in (%*) do call :PROCESS_ONE_ARG %%a
if [%EXEC_WAS_SET%]==[] set EXEC=%DEFAULT_EXEC%

echo %EXEC% %REMAINED_ARGS%
exit /b

:PROCESS_ONE_ARG
set FIRST_ARG_PROCESSED=
if [%FIRST_ARG_PROCESSED%]==[] (
    set FIRST_ARG_PROCESSED=Y
    if [%1]==[py] (
        set EXEC=%PYTHON_EXEC%
        set EXEC_WAS_SET=Y
        exit /b
    )
    if [%1]==[java] (
        set EXEC=%JAVA_EXEC%
        set EXEC_WAS_SET=Y
        exit /b
    )
) 
set REMAINED_ARGS=%REMAINED_ARGS% %*
exit /b
