<!--METADATA {"custom_template_placeholders": {"home_path": "../", "doc_path": ""}} -->

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
artifacts. See the [Home page](../readme.html#installation_all) for more details. 
Here's how the Java version may be installed:

- Place (or clone from the VCS) directory `md2html` (that contains directory `java`) wherever
    you like.
- Define `MD2HTML_HOME` environment variable as the absolute path of directory `md2html`.
- Take or build the executable artifact (see below).

Unlike the Python version, the Java version in this distribution doesn't contain the executable
artifacts. The executable artifact must be built or taken separately.


## Using a release build

The latest release must be available along with the source code. A release is a file named
`md2html-<version>-bin.jar`. It must be placed inside the directory `%MD2HTML_HOME%\java\target\`
(create it if it does not exist). Then the whole distribution must work.


## Building the project

This needs Java Development Kit (JDK, not JRE) 8 or higher and [Maven](https://maven.apache.org/).

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

> __Note.__ The tests and the whole build will fail if the variable `MD2HTML_HOME` is not defined.


## Artifact separate usage

Actually the release artifact `md2html-<version>-bin.jar` may be used separately but it may need
the default template and CSS whose relative locations are hard-coded. Anyway the module may
work if those options are explicitly specified by the arguments.

----------------------------------------------------------------------------------------------------
# Usage

A simple usage example is:

````shell
>java -jar %MD2HTML_HOME%\java\target\md2html-0.1.1-bin.jar -i test.txt
````

This will convert file `test.txt` into file `test.html` using default parameters. For other
options use argument `-h` or run the module without arguments.

----------------------------------------------------------------------------------------------------
# Development

There are no additional notes to the building process description above. The packaging and the
tests are defined in the source code and run automatically when the project is built.

----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------

