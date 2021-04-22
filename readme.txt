[TOC]

----------------------------------------------------------------------------------------------------
# Python Markdown to HTML converter

![](demo/logo.png)

This utility provides an easy way to convert single 
[Markdown](https://daringfireball.net/projects/markdown/) documents into single HTML pages.

It uses __Python-Markdown__ module, see:

- [Official documentation](https://python-markdown.github.io/)
- [PIP Python-Markdown module page](https://pypi.org/project/Markdown/)

This document describes the converter's features, gives usage notes and itself demonstrates a
possible obtainable result.

----------------------------------------------------------------------------------------------------
# Prerequisites

This utility requires [Python 3](https://www.python.org/). The following Python packages must
be installed (in Windows, `pip3` command must be called just `pip`):

````shell
$ pip3 install Markdown
.  .  .
$ pip3 install markdown-del-ins
.  .  .
$ pip3 install markdown-emdash
.  .  .
````

> __Note.__ If this doesn't work, try `python -m pip install ...` (or probably `python3 ...`
> on Linux).

----------------------------------------------------------------------------------------------------
# Installation

Place the directory containing this file wherever you like. Define `MD2HTML_PY_HOME` environment
variable (system-wide of for a user). The latter is not necessary though will make the usage 
much more convenient (this variable will probably also be mentioned below).

File [`md2html.py`](md2html.py) is minimal possible element required. The `templates` directory
contains the HTML template and the CSS used by default. The `win_context_menu` directory 
contains Windows Explorer context menu integration artifacts (see below).

----------------------------------------------------------------------------------------------------
# Usage

The utility provides its usage information in a standard manner:

````shell
>python md2html.py -h
usage: md2html.py [-h] [-i INPUT] [-o OUTPUT] [-t TITLE] [--templates TEMPLATES] [-l LINK_CSS] [-f] [-v] [-r] ...

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


## Double-click script

File [`md2html.bat`](md2html.bat) is used for generating this documentation. It demonstrates
different usage variations. File [`md2html_list.txt`](md2html_list.txt) contains arguments for
a single HTML-file generation per line. The script reads this lines successively and sends the
arguments the Python module. Also the script adds its own arguments to the Python module call.
For example, if `md2html.bat -f` is called then the module will forcefully regenerate the
HTML files.

This script may be executed by a double click from, e.g., the project's directory. It opens 
a command window, does its job and:

- if finished successfully, closes the command window;
- in case of errors/exceptions, leaves the command window open.

If the value of `MD2HTML_PY_HOME` variable is added to the `PATH` then this script will work
from the current directory and process the file `md2html_list.txt` from that directory.


## Windows Explorer context menu

This utility may be integrated into Windows Explorer context menu.

![](demo/windows_context_menu.png)

For this, the batch file 
[`win_context_menu/md2html_prompt.bat`](win_context_menu/md2html_prompt.bat) may be used. 
It opens a command line prompt window and allows to redefine some options. Just pressing
`Enter` will fulfill generation with default options.

To add this context menu item, press `Win`+`R`, type `regedit` and add the following keys
and values (`@` stands for `(Default)`):

````
[HKEY_CURRENT_USER\Software\Classes\*\shell\md2html]
@="Markdown to HTML"
"icon"="X:\\path\\to\\md2html\\win_context_menu\\icon.ico"

[HKEY_CURRENT_USER\Software\Classes\*\shell\md2html\command]
@="\"X:\\path\\to\\md2html\\win_context_menu\\md2html_prompt.bat\" \"%1\""
````

__Note.__ The quotes must be set like this:

![](demo/add_reg_value.png)


## With Git

When working on a project we need to periodically regenerate our HTML documentation. With Git 
we may want to do it automatically on commit. Special argument `--report` was introduced for 
this purpose. It outputs the generated output file path if this file was generated or 
regenerated so that a Git hook can add it into the _stage_.

Here's a Git hook `pre-commit` example (works in Windows too):

````code
#!/bin/bash
grep -v '^\s*$' md2html_list.txt | sed -e 's/\r//' | while read args; do
    result=`echo ${args} | xargs python3 ${MD2HTML_PY_HOME}/md2html.py -r`
    exitcode=${PIPESTATUS[0]}
    result=`echo $result | sed -e 's/\r//'`
    if [ $exitcode -eq 0 ]; then
        if [[ -n $result ]]; then
            echo md2html.py: Adding: $result
            git add -- ${result}
        else
            echo md2html.py: Skipping one file
        fi
    else
        echo Error: $result
        exit 1
    fi
done
````

The file [`md2html_list.txt`](md2html_list.txt) was already mentioned above. Defining environment
variable `MD2HTML_PY_HOME` (globally or for a user) may be a better decision than specifying
the exact full path.

## In Linux

This utility works in Linux. The script for manual HTML generation may be adapted from the
above Git hook example. It may be simplified, `-r` argument must be changed to `-v` and
`git add` command must be removed.

----------------------------------------------------------------------------------------------------
# Templates

By default the utility works based on a predefined empirically developed template that must be
suitable for most basic technical writing. Different template may be created and defined via the 
command line. An example below demonstrates this option.

## CSS

For portability purpose by default CSS is included into the HTML document. Press `Ctrl` + `U`
in Firefox (and probably other browsers) to see haw it looks like in the source code. Command
line arguments allow to redefine this behavior.

<a name="ref_to_custom_template_page"></a>

> Below is an example of a page that was generated using a custom template set. And also CSS 
> was linked instead of being included. Click the arrow to navigate:
> 
> [![](demo/arrow-right.png)](demo/custom_templates.html)

----------------------------------------------------------------------------------------------------
# Feature testing

This document itself demonstrates the available Markdown features of this implementation.
This section provides other examples.

> See the source Markdown files like ([this](readme.txt)) to know how such result can be obtained.

## Lists

The following text will be arranged in a form of a multi-level list with long items in order
to check the indentations and the text flow. It's important to check the indentations on the
__right__.

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

So if we want to just align text we can use a table without a header:

|||||
|---:|---: |---: |---: |
| 10 | 20  | 30  | 40  |
| 50 | 60  | 70  | 80  |
| 90 | 100 | 110 | 120 |

Markdown doesn't allow for different table styles, but some trick may be used to get this
functionality --- specifically, adding an invisible element (an empty `<div>` in this case)
right before the table and then using some simple CSS magic to define the appearance:

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

Text fragments may be marked as:

- ~~deleted~~ (surround with double tildes `~~` from both sides);
- ++inserted++ (surround with double pluses from both sides `++`);
- of cause **bold** (`__bold__` or `**bold**`);
- and *italic* (`_italic_` or `*italic*`).

Three dashes (`---`) may be replaced with an em-dash (`&mdash;`) --- yes, it works!

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
 - and the text must be monospaced
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
