@echo off

java -jar %MD2HTML_HOME%/java/target/md2html-bin.jar --argument-file md2html_args.json %* || pause
