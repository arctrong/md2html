@echo off

set OUTPUT=%USERPROFILE%\md2html_context_menu_integration.reg
set MD2HTML_HOME_MASKED=%MD2HTML_HOME:\=\\%

echo Windows Registry Editor Version 5.00 > %OUTPUT%
echo ;; Adding md2html Windows Explorer context menu integration >> %OUTPUT%
echo. >> %OUTPUT%
echo [HKEY_CURRENT_USER\Software\Classes\*\shell] >> %OUTPUT%
echo. >> %OUTPUT%
echo [HKEY_CURRENT_USER\Software\Classes\*\shell\md2html] >> %OUTPUT%
echo @="Markdown to HTML..." >> %OUTPUT%
echo "icon"="%MD2HTML_HOME_MASKED%\\bin\\context_menu\\icon.ico" >> %OUTPUT%
echo. >> %OUTPUT%
echo [HKEY_CURRENT_USER\Software\Classes\*\shell\md2html\command] >> %OUTPUT%
echo @="\"%MD2HTML_HOME_MASKED%\\bin\\context_menu\\md2html_prompt.bat\" -i \"%%1\"" >> %OUTPUT%
echo. >> %OUTPUT%
echo [HKEY_CURRENT_USER\Software\Classes\*\shell\md2html_fast] >> %OUTPUT%
echo @="Markdown to HTML (no prompt)" >> %OUTPUT%
echo "icon"="%MD2HTML_HOME_MASKED%\\bin\\context_menu\\icon.ico" >> %OUTPUT%
echo. >> %OUTPUT%
echo [HKEY_CURRENT_USER\Software\Classes\*\shell\md2html_fast\command] >> %OUTPUT%
echo @="\"%MD2HTML_HOME_MASKED%\\bin\\context_menu\\md2html_prompt_fast.bat\" -i \"%%1\"" >> %OUTPUT%
echo. >> %OUTPUT%

echo The output file is: %OUTPUT%

