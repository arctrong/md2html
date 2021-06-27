@echo off

python -m unittest discover %* --start-directory=%~dp0
