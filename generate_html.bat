@echo off

for /f "delims=" %%a in (generate_html_list.txt) do call python3 generate_html.py %* %%a
