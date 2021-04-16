@echo off

for /f "delims=" %%a in (demo\demo_list.txt) do call python3 md2html.py %* %%a
