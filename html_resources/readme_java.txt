[TOC]

----------------------------------------------------------------------------------------------------
# Introduction

This is the Java implementation of the Markdown to HTML converter.

This document describes the implementation-specific details. The whole description is given
on the [Home page](../readme.html).

This implementation uses module [vsch/flexmark-java](https://github.com/vsch/flexmark-java) 
for converting _Markdown_ text to HTML.

----------------------------------------------------------------------------------------------------
# Prerequisites

Java runtime (JRE) 8 or higher.

----------------------------------------------------------------------------------------------------
# Installation

The Java implementation is shipped together with other implementations that share some common
artifacts like templates, scripts, and documentation. First see the [Home page](../readme.html) 
for the common installation instructions.

Unlike the Python version, Java version in this distribution doesn't contain the executable
artifacts. They must be built or taken separately.


## Using a release build

The latest release must be available along with the source code. A release is a file named
`md2html-<version>-bin.jar`. It must be placed inside the directory `%MD2HTML_HOME%\java\target\`
(create it if it does not exist). Then the whole distribution must work.


## Building the project

This needs Java Development Kit (JDK) 8 or higher and [Maven](https://maven.apache.org/).

In a command line terminal execute:

````shell
>cd %MD2HTML_HOME%\java

>release.bat
````

The following must be output as the process ends:

````shell
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
````

File `md2html-<version>-bin.jar` must appear in the directory `%MD2HTML_HOME%\java\target\`.


## Artifact separate usage

Actually the release artifact `md2html-<version>-bin.jar` may be used separately but it may need
the default template and CSS whose relative locations are hard-coded. Anyway the module may
work if those options are explicitly specified by the arguments.

----------------------------------------------------------------------------------------------------
# Usage

The utility provides its usage information in a standard manner:

````shell
>java -jar %MD2HTML_HOME%\java\target\md2html-0.1.1-bin.jar

usage: java Md2Html [-h] [-i <arg>] [-o <arg>] [-t <arg>] [--templates <arg>]
       [--link-css <arg>] [--include-css <arg>] [--no-css] [-f] [-v] [-r]

 -h,--help                show this help message and exit
 -i,--input <arg>         input Markdown file name (mandatory)
 -o,--output <arg>        output HTML file name, defaults to input file name
                          with '.html' extension
 -t,--title <arg>         the HTML page title, if omitted there will be an empty
                          title
    --templates <arg>     custom template directory
    --link-css <arg>      links CSS file, multiple entries allowed
    --include-css <arg>   includes content of the CSS file into HTML, multiple
                          entries allowed
    --no-css              creates HTML with no CSS. If no CSS-related arguments
                          is specified, the default CSS will be included
 -f,--force               rewrites HTML output file even if it was modified
                          later than the input file
 -v,--verbose             outputs human readable information messages
 -r,--report              if HTML file is generated, outputs the path of this
                          file, incompatible with -v
````

----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------

