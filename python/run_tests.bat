@echo off

py -m unittest discover %* --start-directory=%~dp0
