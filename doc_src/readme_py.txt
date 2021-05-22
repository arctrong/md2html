<!--METADATA {"custom_template_placeholders": {"home_path": "../", "doc_path": ""}} -->

[TOC]

----------------------------------------------------------------------------------------------------
# Introduction

This is the Python implementation of the Markdown to HTML converter.

This document describes the implementation-specific details. The whole description is given
on the [Home page](../readme.html).

This implementation uses __Python-Markdown__ module, see:

- [Official documentation](https://python-markdown.github.io/)
- [PIP Python-Markdown module page](https://pypi.org/project/Markdown/)

----------------------------------------------------------------------------------------------------
# Prerequisites

This implementation requires [Python 3](https://www.python.org/). The following Python packages 
must be installed:

````shell
$ pip3 install Markdown
.  .  .
$ pip3 install markdown-emdash
.  .  .
````

> __Notes.__ 1. If this doesn't work, try to replace `pip3` with `python -m pip` (or probably 
> also use `python3` instead of `python` on Linux).
> 2. in Windows, `pip3` command must be called just `pip`.

----------------------------------------------------------------------------------------------------
# Installation

The Python implementation is shipped together with other implementations that share some common
artifacts like templates, scripts, and documentation. See the [Home page](../readme.html) 
for installation instructions.

> Actually the file `python/md2html.py` may be used separately but it may need the default
> template and CSS whose relative locations are hard-coded. Anyway the module may work if those
> options are explicitly specified by the arguments.

----------------------------------------------------------------------------------------------------
# Usage

The utility provides its usage information in a standard manner:

````shell
>python %MD2HTML_HOME%\python\md2html.py
usage: md2html.py [-h] -i INPUT [-o OUTPUT] [-t TITLE] [--template TEMPLATE]
                  [--link-css LINK_CSS] [--include-css INCLUDE_CSS] [--no-css]
                  [-f] [-v] [-r]

Converts Markdown document into HTML document.

optional arguments:
  -h, --help            show this help message and exit
  -i INPUT, --input INPUT
                        input Markdown file name (mandatory)
  -o OUTPUT, --output OUTPUT
                        output HTML file name, defaults to input file name with
                        '.html' extension
  -t TITLE, --title TITLE
                        the HTML page title, if omitted there will be an empty
                        title
  --template TEMPLATE   custom template directory
  --link-css LINK_CSS   links CSS file, multiple entries allowed
  --include-css INCLUDE_CSS
                        includes content of the CSS file into HTML, multiple
                        entries allowed
  --no-css              creates HTML with no CSS. If no CSS-related arguments is
                        specified, the default CSS will be included
  -f, --force           rewrites HTML output file even if it was modified later
                        than the input file
  -v, --verbose         outputs human readable information messages
  -r, --report          if HTML file is generated, outputs the path of this
                        file, incompatible with -v
````

----------------------------------------------------------------------------------------------------
# Development

To run tests execute:

````shell
>%MD2HTML_HOME%\python\test.bat

````

The output must end with something like this:

````shell
........................................
----------------------------------------------------------------------
Ran 40 tests in 0.021s

OK
````

----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------
