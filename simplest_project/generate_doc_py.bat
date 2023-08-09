@echo off

python %MD2HTML_HOME%/python/md2html.py --argument-file md2html_args.json %* || pause
