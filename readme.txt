<!--VARIABLES {"noPageTitle": true}--> 

![Logo](<!--path pict-->logo.png "md2html"){.floatRight}

**M<sub>2</sub>H** creates structured, well-formatted static HTML sites with minimal effort.
It can be used for many kinds of writing projects.

This documentation was written entirely using **M<sub>2</sub>H** and serves as a real-world
example of the tool in action.

[TOC]

<p style="clear: both;"></p>


# How it works

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
    

# Features

**M<sub>2</sub>H** automates the conversion of Markdown texts into HTML pages and provides templates
and styles that are either ready to use or easy to adapt and extend.

In addition, it supports processing whole document sets, organizing the output, and producing
self-contained static HTML documentation that needs only a browser to be viewed.

Finally, **M<sub>2</sub>H** includes plugins that automate common tasks and help the documentation
look professional.

**M<sub>2</sub>H** doesn't impose many restrictions on project organization and lets users define
their own configuration. Still, it provides recommended project structures described in this
manual and implemented in the quick start scripts.


# Implementation

**M<sub>2</sub>H** is a command line utility available in two versions: Python and Java. They work
mostly the same way, share the same command line syntax, and process the same inputs. The versions
are independent and can be used separately, but they are shipped together as source code and share
common artifacts (templates, styles, scripts, and this documentation). This manual describes both
versions.


