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
$ pip3 install markdown markdown-emdash
````

> __Notes.__ 1. If this doesn't work, try to replace `pip3` with `python -m pip` (or probably 
> also use `python3` instead of `python` on Linux).
> 2. in Windows, `pip3` command must be called just `pip`.

----------------------------------------------------------------------------------------------------
# Installation

The Python implementation is shipped together with other implementations that share some common
artifacts. See the [Home page](../readme.html#implementation_specific_documents_links) for more
details. Here's how the Python version may be installed:

- Place (or clone from the VCS) directory `md2html` (that contains directory `python`) wherever
    you like.
- Define `MD2HTML_HOME` environment variable as the absolute path of directory `md2html`.
- That's it, provided that the prerequisites are met.

> Actually the file `python/md2html.py` may be used separately but it may need the default
> template and CSS whose relative locations are hard-coded. Anyway the module may work if those
> options are explicitly specified by the arguments.

----------------------------------------------------------------------------------------------------
# Usage

A simple usage example is:

````shell
>python %MD2HTML_HOME%/python/md2html.py -i test.txt
````

This will convert file `test.txt` into file `test.html` using default parameters. For other
options use argument `-h` or run the module without arguments.

----------------------------------------------------------------------------------------------------
# Development

There are no special packaging actions required. The file `python/md2html.py` is a self-contained
executable module.

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
