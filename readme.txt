<!--VARIABLES {"noPageTitle": true}--> 

<p><img src="<!--path pict-->logo.png" title="md2html" class="floatRight" /></p>

**M<sub>2</sub>H** creates structured well formatted HTML documentation with minimal effort and may be
used for different types of writing works.

The first and the most telling example is this documentation that was written solely using this
tool.

[TOC]

<p style="clear: both;"></p>


# How it works?

You write a Markdown text like this:

````
!!! note
    A **monad** is just a _monoid_ in the category of `endofunctors`.
````

**M<sub>2</sub>H** converts it into HTML code like this:

````wrapped-html
<div class="admonition note">
<p class="admonition-title">Note</p>
<p>A <strong>monad</strong> is just a <em>monoid</em> in the category of <code>endofunctors</code>.</p>
</div>
````

Then it includes this code into a specially prepared HTML template and produces the page that
looks like this:

!!! note
    A **monad** is just a _monoid_ in the category of `endofunctors`.
    
There are more details described in the [Overview section](<!--page overview-->).


<!--index Markdown -->
# Markdown

[Markdown](<!--page refs-->#markdown) is a formatting syntax whose
main goal is making texts that are as readable as possible in *plain text*.

This syntax is very simple and allows writing very fast. It has enough features for writing
a variety of document kinds. Because of its simplicity it's quite restricted and contains
just a very small subset of HTML features, but it allows direct inclusions of HTML code that
helps achieve more complex result when it's required.


# Features

**M<sub>2</sub>H** automates conversion of Markdown texts into HTML pages and provides templates
and styles that are ether ready to use or may be easily adapted and extended for certain cases.

Apart of that, it provides means for processing whole sets of documents, organizing the output
and producing a self-contained static HTML documentation that needs only a browser to be viewed.

In the end, this tool has a set of plugins that automate and simplify typical tasks and make
the documentation look more professionally.

**M<sub>2</sub>H** doesn't lay much restrictions on the documentation project's organization and
lets the users make their own configuration. There are recommendations on the project structure
described in this manual and used for its creation.


# Implementation

**M<sub>2</sub>H** is a command line utility that has two versions: in Python and in Java. They
work mostly the same way, have the same command line syntax, process the same input, are shipped
together as source code and share some common artifacts like templates and styles, command
scripts, and this documentation. Despite this, the versions are independent and can be
used separately. This manual describes the both versions.



