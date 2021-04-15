[TOC]

----------------------------------------------------------------------------------------------------
# Python Markdown to HTML converter

![](demo/logo.png)

This utility provides an easy way to convert single Markdown documents into single HTML pages.

It uses __Python-Markdown__ module. See:

- [Official documentation](https://python-markdown.github.io/)
- [PIP Python-Markdown module page](https://pypi.org/project/Markdown/)

This document describes the utility's features, gives usage notes and itself demonstrates a
possible obtainable result.

----------------------------------------------------------------------------------------------------
# Prerequisites

This utility requires the following Python packages:

````shell
$ pip3 install Markdown
.  .  .
Installing collected packages: zipp, typing-extensions, importlib-metadata, Markdown
Successfully installed Markdown-3.3.4 importlib-metadata-3.10.1 typing-extensions-3.7.4.3 zipp-3.4.1

$ pip3 install markdown-del-ins
.  .  .
Installing collected packages: typing-extensions, zipp, importlib-metadata, markdown, markdown-del-ins
Successfully installed importlib-metadata-3.10.1 markdown-3.3.4 markdown-del-ins-1.0.0 typing-extensions-3.7.4.3 zipp-3.4.1

$ pip3 install markdown-emdash
.  .  .
Installing collected packages: markdown-emdash
Successfully installed markdown-emdash-0.1.0
````

----------------------------------------------------------------------------------------------------
# Usage

````shell
>python generate_html.py -h
usage: generate_html.py [-h] [-i INPUT] [-o OUTPUT] [-t TITLE] [--templates TEMPLATES] [-l LINK_CSS] [-f] [-v] [-r] ...

Converts Markdown document into HTML document.

positional arguments:
                        positionals for -i, -o, and -t, incompatible with corresponding named arguments

optional arguments:
  -h, --help            show this help message and exit
  -i INPUT, --input INPUT
                        input Markdown file name (mandatory)
  -o OUTPUT, --output OUTPUT
                        output HTML file name, defaults to input file name with '.html' extension
  -t TITLE, --title TITLE
                        the HTML page title, if omitted there will be an empty title
  --templates TEMPLATES
                        custom template directory
  -l LINK_CSS, --link-css LINK_CSS
                        links CSS file, if omitted includes the default CSS into HTML
  -f, --force           rewrites HTML output file even if it was modified later than the input file
  -v, --verbose         outputs human readable information messages
  -r, --report          if HTML file is generated, outputs the path of this file, incompatible with -v

Simplified argument set may be used: <input file name> <output file name> <page title>
````
 
File [`generate_html.bat`](generate_html.bat) demonstrates different usage variations. It
transmits its arguments directly to the `generate_html.py` module. For example, 
if `generate_html.bat -vf` is called then the module will forcefully regenerate the HTML files
and will do it verbosely.

## Double-click script

This script may be executed by a double click from, e.g., the project's directory. It opens 
a command window, does its job and:

- if finished successfully closes the command window;
- in case of errors/exceptions leaves the command window open.

`generate_html_list.txt` is a file which contains arguments for a single HTML-file generation
per line.

Environment variable `MD2HTML_PY_HOME` may be defined system-wide or for a current user.

````code
@echo off

set SUCCESS=

for /f "delims=" %%a in (generate_html_list.txt) do call :GENERATE_ONE_HTML %%a

if not [%SUCCESS%]==[Y] pause

exit /b

:GENERATE_ONE_HTML
set SINGLE_SECCESS=N
echo Generating: %2
call python %MD2HTML_PY_HOME%\generate_html.py -v %* && set SINGLE_SECCESS=Y
if not [%SINGLE_SECCESS%]==[Y] (
    set SUCCESS=N
) else (
    echo Done
    if not [%SUCCESS%]==[N] (
        set SUCCESS=Y
    )
)
exit /b
````

## With Git

When working on a project we need to periodically regenerate our HTML documentation. With Git 
we may want to do it automatically on commit. Special argument `--report` was introduced for 
this purpose. It outputs the generated output file path if this file was generated or 
regenerated so that a Git hook can add it into stage.

Here's a Git hook pre-commit example (works in Windows too):

````code
#!/bin/bash
grep -v '^\s*$' generate_html_list.txt | sed -e 's/\r//' | while read args; do
    result=`echo ${args} | xargs python3 generate_html.py -r`
    exitcode=${PIPESTATUS[0]}
    result=`echo $result | sed -e 's/\r//'`
    if [ $exitcode -eq 0 ]; then
        if [[ -n $result ]]; then
            echo generate_html.py: Adding: $result
            git add -- ${result}
        else
            echo generate_html.py: Skipping one file
        fi
    else
        echo Error: $result
        exit 1
    fi
done
````

[`generate_html_list.txt`](generate_html_list.txt) is a file which contains arguments for a
single HTML-file generation per line. This list is used also by the manual HTML generation
script.

## In Linux

This utility works in Linux. The script for manual HTML generation may be adapted from the
above Git hook example. Among other required changes the main one is removing `git add`
command.

----------------------------------------------------------------------------------------------------
# Templates

This utility works with a predefined set of empirically developed templates that must be suitable
for most basic technical writing. Different templates may be created and defined via the 
command line. An example below demonstrates this option.

## CSS

For portability purpose by default CSS are included into the HTML document. Press `Ctrl` + `U`
in Firefox (and probably other browsers) to see haw it looks like. Command line arguments
allow to redefine this behavior.

<a name="ref_to_custom_template_page"></a>

> Below is an example of a page that was generated using a custom template set. And also CSS 
> were linked instead of being included. Click the arrow to navigate:
> 
> [![](demo/arrow-right.png)](demo/custom_templates.html)

----------------------------------------------------------------------------------------------------
# Feature testing

This document itself demonstrates the available Markdown features of this implementation.
This section provides other examples for solely testing and demonstration purpose.

> See the source Markdown files ([like this](readme.txt)) to know how such result can be obtained.

## Lists

The following text will be arranged in a form of a multi-level list with long items in order
to check the indentations and other appearance aspects. It's important to check the indentations 
on the __right__.

- This is the __first__ long list item that is going to be wrapped into several lines. So that we 
  can visually check the text flow and indentations, especially on the right.
    1. This is the first __ordered__ list subitem of the first list item. With it we want to 
       check text flow and indentations not only of the higher level list items but of subitems
       as well.
        - And at least let's check third-level items. This is the __first__ third-level items 
          subitem of the the first second-level subitem of the first first-level item of the
          multi-level list.
        - This is the __second__ third-level items subitem of the the first second-level subitem
          of the first first-level item of the multi-level list.
    1. This is the second __ordered__ list subitem of the first list item. With it we want to 
       check text flow and indentations not only of the higher level list items but of subitems
       as well.
- This is the __second__ long list item that is going to be wrapped into several lines. So that we 
  can visually check the text flow and indentations, especially on the right.

If we temporarily add the CSS rule:

````code
p, li {
    border: solid 1px fuchsia;
}
````

we will see:

![](demo/list_text_flow.png)

So the text doesn't overflow the limits.

## Lists in blockquotes

> Let's now have a look at how a list behaves in a `blockquote`. Also we can check the 
> text flow and the indentations of simple paragraphs like this.
>
> - As before this is the _first_ higher level item of the list. _Italic_ is applied just to
>   demonstrate the other formatting option.
>     1. This is the first _ordered_ list subitem of the first higher-level list item.
>     1. This is the second _ordered_ list subitem of the first higher-level list item.
> - This is the _second_ higher level item of the list.

As we can see there's no overflow in this case too:

![](demo/blockquote_list_text_flow.png)

## Tables

Tables may be used for representing tabular data and for text alignment sometimes. In this
implementation light table style is chosen as the default.

Item No | Name | Description | Price
:------:|------|-------------|-----:
1       | Chair | Kitchen chair |  87.50
2       | Table | Kitchen table | 450.00
3       | Lamp  | Standard lamp | 120.75

As Markdown doesn't allow for different table styles, some CSS trick may be used to get this
functionality --- specifically, adding an invisible element before the table and then using some
simple CSS magic to define the appearance:

````code
<div class="tablePlated"></div>
````

Then we can get styles like this:

<div class="tablePlated"></div>

|Item No | Name | Description | Price|
|:------:|------|-------------|-----:|
|1       | Chair | Kitchen chair |  87.50|
|2       | Table | Kitchen table | 450.00|
|3       | Lamp  | Standard lamp | 120.75|

and this (`class="tableGridded"`):

<div class="tableGridded"></div>

Item No | Name | Description | Price
:------:|------|-------------|-----:
1       | Chair | Kitchen chair |  87.50
2       | Table | Kitchen table | 450.00
3       | Lamp  | Standard lamp | 120.75

## Text effects

Text fragments may be marked as ~~deleted~~ (surround with double tildes `~~` from both sides)
and ++inserted++ (surround with double pluses from both sides `++`). Three dashes (`---`) may be
replaced with an em-dash (`&mdash;`) --- yes, it works! Of cause __bold__ (`__bold__` or
`**bold**`) and _italic_ (`_italic_` or `*italic*`) are also supported. 

## Code blocks

Unmarked code block. Visually corresponds to `inline` code fragments:

````
This code block represents information:
 - like `inline` code fragments
 - but of bigger size
 - and when we need to preserve line breaks
````

Light code block (marked as `text`):

````text
This code block is used:
 - when we need no background and borders
 - and we need to preserve line breaks
````

Shell code block (marked as `shell`):

````shell
$ echo "This block demonstrates operations in a shell"
This block demonstrates operations in a shell
````

Program language code block (marked as `code`):

````code
System.out.println("This block demonstrates a source code in a program language.");
````

----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------
