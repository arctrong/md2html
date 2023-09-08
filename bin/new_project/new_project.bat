@echo off

set CANNOT_PROCEED=

call :check_file_or_dir doc_src
call :check_file_or_dir doc
call :check_file_or_dir md2html_args.json
call :check_file_or_dir generate_doc_py.bat
call :check_file_or_dir generate_doc_java.bat

if [%CANNOT_PROCEED%]==[Y] (
    echo Some problems found (see above^). Nothing has been done
    exit /b
)

xcopy %MD2HTML_HOME%\bin\new_project\doc_src doc_src\ /e
xcopy %MD2HTML_HOME%\doc_src\templates\multipage.html doc_src\templates\
xcopy %MD2HTML_HOME%\bin\new_project\doc doc\ /e
xcopy %MD2HTML_HOME%\doc\layout doc\layout\ /e
xcopy %MD2HTML_HOME%\doc\themes\light doc\themes\light\ /e
xcopy %MD2HTML_HOME%\bin\new_project\md2html_args.json
xcopy %MD2HTML_HOME%\bin\new_project\readme.txt
xcopy %MD2HTML_HOME%\generate_doc_py.bat
xcopy %MD2HTML_HOME%\generate_doc_java.bat
xcopy %MD2HTML_HOME%\doc\favicon.png doc\

exit /b

:check_file_or_dir
if exist %1 (
    echo File or directory '%1' already exists
    set CANNOT_PROCEED=Y
)
exit /b
