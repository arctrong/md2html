[TOC]

----------------------------------------------------------------------------------------------------
# Java Markdown to HTML converter #

![](demo/logo.png)

This is a Java version of _Markdown to HTML converter_. It works mostly the same way as the 
[_Python version_](readme.html) and is intended to be used interchangeably with the
[_Python version_](readme.html) on the same source files, templates, and configuration files.

This document gives short descriptions and demonstrates the capabilities of the utility.

----------------------------------------------------------------------------------------------------
# Prerequisites

Java runtime (JRE) 8 or higher.

----------------------------------------------------------------------------------------------------
# Installation

- Unpack the release archive wherever you like.
- Define `MD2HTML_PY_HOME` environment variable (system-wide of for a user) as the path to the
    directory containing the `templates` directory. Unlike the _Python version_ this is required.
- Optionally define `MD2HTML_JAVA_HOME` environment variable as the path to the directory
    containing the file `md2html-<version>.jar`.

----------------------------------------------------------------------------------------------------
# Usage

````shell
>java -jar %MD2HTML_JAVA_HOME%\md2html-<version>.jar

usage: java Md2Html [-h] [-i <arg>] [-o <arg>] [-t <arg>] [--templates <arg>]
       [-l <arg>] [-f] [-v] [-r]
       , [input], [output], [title]

Positional arguments: [input], [output], [title]
 -h,--help              show this help message and exit
 -i,--input <arg>       input Markdown file name (mandatory)
 -o,--output <arg>      output HTML file name, defaults to input file name with
                        '.html' extension
 -t,--title <arg>       the HTML page title, if omitted there will be an empty
                        title
    --templates <arg>   custom template directory
 -l,--link-css <arg>    links CSS file, if omitted includes the default CSS into
                        HTML
 -f,--force             rewrites HTML output file even if it was modified later
                        than the input file
 -v,--verbose           outputs human readable information messages
 -r,--report            if HTML file is generated, outputs the path of this
                        file, incompatible with -v
````

The syntax is the same as of the [_Python version_](readme.html#md2html_py_usage).


## Double-click script

See [`md2html_java.bat`](md2html_java.bat). Works the same way as for the
[_Python version_](readme.html#md2html_py_double_click_script).

----------------------------------------------------------------------------------------------------
# Feature testing

<a name="md2html_java_template"></a>

## Template

This page is generated using a custom template with CSS linking.
[Here](java_demo/default_template.html) is an example of a page generated with default template
with CSS embedding.

<a name="md2html_java_malformed_placeholders"></a>

Unlike [_Python version_](readme.html), this Java utility doesn't accept malformed placeholders
in templates. But it correctly processes well-formed placeholders for which corresponding keys
are not defined. [Here](java_demo/absent_placeholder.htm) is an example.


## Blockquotes

> This is a `blockquote` with a link image inside.
> 
> [![DEFAULT TEMPLATE USAGE DEMO](demo/arrow-right.png)](java_demo/default_template.html
"Default template usage demo")


## Images

An image usage is demonstrated above (the yellow arrow). In this example `alt` and `title` 
attributes are filled.


## Lists

This is a multilevel list:

- This is the __first__ first-level list item.
    1. This is the first __ordered__ list subitem of the first list item.
        - This is the __first__ third-level subitem of the the first second-level subitem of
            the first first-level item of the multilevel list.
        - This is the __second__ third-level items subitem of the the first second-level subitem
          of the first first-level item of the multi-level list.
    1. This is the second __ordered__ list subitem of the first list item.
- This is the __second__ first-level list item.


## Lists in blockquotes

> Let's now have a look at how a list behaves in a `blockquote`:
>
> - The _first_ higher level item of the list.
>     1. The first _ordered_ list subitem.
>     1. The second _ordered_ list subitem.
> - The _second_ higher level item of the list.


## Tables

Default table style.

Item No | Name | Description | Price
:------:|------|-------------|-----:
1       | Chair | Kitchen chair |  87.50
2       | Table | Kitchen table | 450.00
3       | Lamp  | Standard lamp | 120.75

Default table style without a header:

|||||
|---:|---: |---: |---: |
| 10 | 20  | 30  | 40  |
| 50 | 60  | 70  | 80  |
| 90 | 100 | 110 | 120 |

"Plated" table style:

<div class="tablePlated"></div>

|Item No | Name | Description | Price|
|:------:|------|-------------|-----:|
|1       | Chair | Kitchen chair |  87.50|
|2       | Table | Kitchen table | 450.00|
|3       | Lamp  | Standard lamp | 120.75|

"Gridded" table style:

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

Three dashes (`---`) will be replaced with an em-dash (`&mdash;`) --- yes, it works!

## Code blocks

Unmarked code block. Visually corresponds to `inline` code fragments:

    This code block represents information:
     - like `inline` code fragments
     - but of bigger size
     - and when we need to preserve line breaks

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

----------------------------------------------------------------------------------------------------


