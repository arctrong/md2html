<!--METADATA {"title": "Markdown to HTML converter", 
"custom_template_placeholders": {"home_path": "", "doc_path": "doc/"}} -->
[TOC]

----------------------------------------------------------------------------------------------------
# Introduction

![LOGO](doc/pict/logo.png "logo")

This is a command line utility that converts _Markdown_ documents into static HTML pages. 
It is provided with HTML templates, CSS, and scripts for batch processing and integration.

This documentation is created using this utility and itself demonstrates a possible obtainable
result.

## About Markdown

This is a simple example of _Markdown_ syntax:

````
> A **monad** is just a _monoid_ in the category of `endofunctors`.
````

This plain text will be converted into the following HTML code:

````
<blockquote>
<p>A <strong>monad</strong> is just a <em>monoid</em> in the category of <code>endofunctors</code>.</p>
</blockquote>
````

that will look like this:

> A **monad** is just a _monoid_ in the category of `endofunctors`.

Note that the closing angle bracket (`>`) is also a part of the syntax. It marks up the line
as a `<blockquote>` HTML element.

It must be emphasized that _Markdown_ is intended to be __easy-to-write__ and __easy-to-read__
in plain text. It contains just a very small subset of HTML features but allows inclusion of
direct HTML code.

[This site](https://daringfireball.net/projects/markdown/) (among others) may be viewed for
more details about _Markdown_ and its syntax.


## Security considerations

<a name="html_code_inclision"></a>

This utility doesn't restrict HTML code inside the Markdown texts. Particularly, these texts 
may contain JavaScript that will be translated unchanged into the generated HTML page. This
must not be a problem for personal use but probably may be a security issue when converting and
publishing source texts from untrusted third-parties. Click the image below to see how it may
look like.

<img src="doc/pict/box.png" style="cursor: pointer;" onclick="alert(
'This is just a message but might be any JavaScript code.');" />

----------------------------------------------------------------------------------------------------
# Implementations

This utility has two _implementations_ (they will be called _versions_ sometimes in this
documentation): in Python and in Java. They work mostly the same way, have the same command line 
syntax, process the same input, are shipped together as source code and share some common
artifacts like templates, scripts, and this documentation. Despite this,  the versions are not
interdependent and can be used separately. This document contains information that is common for
all implementations. Implementation-specific details (like system requirements and installation)
are described in separate documents:

<a name="implementation_specific_documents_links"></a>

- [for Python version](doc/readme_py.html)
- [for Java version](doc/readme_java.html)

----------------------------------------------------------------------------------------------------
<a id="installation_all"></a>

# Installation

The Python version is ready to use though it needs the Python 3 execution environment and some
Python packages to be installed. Java version is provided in source code; the executable artifacts
are provided separately as release builds, or prepared users may build them themselves. The common
installation sequence is:

- Place (or clone from the VCS) directory `md2html` (that contains this file) wherever you like.
- Define `MD2HTML_HOME` environment variable as the absolute path of directory `md2html`.
- See corresponding [implementation-specific documents](#implementation_specific_documents_links)
    for further installation instructions.

----------------------------------------------------------------------------------------------------
# Usage

A simple usage example for the Python version is:

````shell
>python %MD2HTML_HOME%/python/md2html.py -i test.txt
````

This will convert file `test.txt` into file `test.html` using default parameters. The Java version
usage is similar. 

The other options are:

````shell
>python %MD2HTML_HOME%/python/md2html.py -h
usage: md2html.py [-h] [-i INPUT] [-o OUTPUT] [-t TITLE] [--template TEMPLATE]
                  [--link-css LINK_CSS] [--include-css INCLUDE_CSS] [--no-css]
                  [-f] [-v] [-r]

Converts Markdown document into HTML document.

optional arguments:
  -h, --help            shows this help message and exits
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


## Bulk processing

File `bin/md2html_batch.bat` may be used for generating several HTML pages in one run. It uses
the _list file_, the file `md2html_list.txt` in the current directory. The list file contains
arguments for a single HTML-file generation per line. Here's an example:

````
-i index.txt -o doc/index.html -t "Home page"
-i about.txt -o doc/about.html -t "About this site"
````

The first optional argument specifies the utility implementation. The other arguments will be
sent directly to the executable module. With the following example:

````shell
>%MD2HTML_HOME%\bin\md2html_batch.bat java -f
````

the Java version will be used and all HTML-files will be forcefully regenerated. If the first 
argument is nether `py` nor `java` then the Python version is used.

The above command may be executed in any directory and it will process the list file in that
directory. If `%MD2HTML_HOME%\bin` is added to the `PATH` then this prefix will not be required.


## Double-click script

File `generate_html.bat` may be run by double-click from Windows file explorer. It works the same
way as script `md2html_batch.bat` except it doesn't finish in case of errors so the command
window remains open. This scripts is very small and is intended to be copied to a project's
directory where it will process the project's list file.


## Windows Explorer context menu

`bin/win_context_menu` directory contains artifacts for integration into Windows Explorer
context menu:

![WINDOWS_EXPOLORER_CONTEXT_MENU](doc/pict/windows_context_menu.png)

It opens a command line prompt window and allows to redefine some options. Just pressing
`Enter` will fulfill generation with default options.

To add this context menu item, open the Windows Registry editor (press `Win`+`R`, type `regedit`
and press `Enter`) and add the following keys and values:

````
[HKEY_CURRENT_USER\Software\Classes\*\shell\md2html]
@="Markdown to HTML"
"icon"="X:\\path\\to\\md2html\\win_context_menu\\icon.ico"

[HKEY_CURRENT_USER\Software\Classes\*\shell\md2html\command]
@="\"X:\\path\\to\\md2html\\bin\\context_menu\\md2html_prompt.bat\" -i \"%1\""
````

__Note.__  1. `@` stands for `(Default)` value name. 2. `py` or `java` may be added before `-i`.
3. The quotes must be set like this:

![](doc/pict/reg_value.png)


## Typical project structure

The following structure may be suggested for a documented project:

````shell
$ tree -L 2 --charset=ascii --dirsfirst
.
|-- doc
|   |-- pict
|   |   |-- favicon.png
|   |   `-- image1.png
|   |-- doc1.html
|   |-- doc2.html
|   `-- styles.css
|-- doc_src
|   |-- templates
|   |   |-- custom.html
|   |   `-- default.html
|   |-- doc1.txt
|   `-- doc2.txt
|-- doc0.html
|-- doc0.txt
|-- generate_html.bat
`-- md2html_list.txt
````

- `doc0.txt` and `doc0.html` are the Markdown document and its corresponding generated HTML
    version that we want to have in the project's root;
- `doc` directory along with file `doc0.html` contains the whole project's documentation;
- `doc_src` directory contains all source files required for producing the project's
    documentation (except file `doc0.txt` if we want it to be located in the project's root);
- `generate_html.bat` --- the double-click script for the whole HTML documentation regeneration;
- `md2html_list.txt` --- the project's list file.

Some elements may be omitted if they are not required.


## Git hook script

Along with periodical manual HTML files regeneration a Git _hook_ may be used to do it
automatically on commit. Special argument `--report` was introduced for this purpose. It outputs
the generated output file path if this file was generated or regenerated so that a Git hook can
add it into the _stage_. Here's a Git hook `pre-commit` example (works in Windows too):

````code
#!/bin/bash
grep -v '^\s*$' md2html_list.txt | sed -e 's/\r//' | while read args; do
    result=`echo ${args} | xargs python3 ${MD2HTML_HOME}/python/md2html.py -r`
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

This script uses the list file `md2html_list.txt` that has been already mentioned in this
document. To add this hook create file `pre-commit` with the above content in directory
`.git/hooks` of your Git repository.


## On Linux

The Java and the Python executables are platform-independent so they work on Linux. The batch
processing scripts are also provided in *Bash shell* syntax. The file explorer integration script
is not yet ported but it may be dependent on the desktop environment and is quite straightforward,
so prepared Linux users may port the existing `bin/context_menu/md2html_prompt.bat` script
themselves.

----------------------------------------------------------------------------------------------------
# Source input elements

To generate a project's documentation or other kind of HTML-based set of documents the 
following source elements must be prepared and defined:

- source Markdown texts that may reference each other and the other content like images. 
    These texts will typically define the inner HTML content, not the complete HTML page;
- one or more templates that define the HTML page constant part and will be populated with 
    the contents generated out of the source texts. There are other replacement options that
    allow for more flexibility;
- CSS rules that define the result documents appearance and itself may be defined in different
    ways.

Following are these elements described in more details.


## Page metadata

Along with its target content a source Markdown text may contain page _metadata_ that is not a
part of the Markdown syntax. The format of the page metadata is:

````
<!--METADATA {
"title": "My title",
"custom_template_placeholders": {"key1": "value1", "key2": "value2"}
}-->
````

> __NOTE.__ __1.__ The page metadata may affect the HTML generation process and the final HTML 
> document in the end. But it is ignored as a part of the source document, so it will not 
> literally appear in the generated HTML code (though it anyway would not be visible as it's,
> in fact, an HTML/XML comment).
> 
> __2.__ The page metadata processing will not fail the page generation. If there are incorrect 
> fragments in metadata, reasonable attempts will be done to recognize the correct elements, 
> all the other perts will be ignored. If verbose mode is on then a warning messages will be
> output to the console.

The page metadata section must be the first non-space text in the source document, otherwise it
will be ignored (and literally left as the the source text). The `METADATA` keyword is
case-insensitive and must follow directly after the opening marker `<!--` without
any space. The metadata content must be a valid _JSON_ text, that means that the keys are
case-sensitive and must be enclosed in double quotes. Also the root element must be an object
(i.e. in curly braces).

> __NOTE!__ Opening `<!--` and closing markers `-->` must not be used inside  the metadata
> section. Also consecutive hyphens `--` inside HTML comments probably may be a problem in some
> browsers. In JSON strings, Unicode entities may be used to resolve these issues, i.e. string
> `"<!\u002D-text-\u002D>"` will be interpreted as `"<!--text-->"`. Still, depending on the 
> page content and the context, opening and closing markers, even when escaped in JSON, may 
> cause unexpected result. Check it first if you really need to use these symbols.

The following metadata parameters are supported:

- `title` of type string (i.e. it must always be in double quotes), defines the default page
    title. The title defined by the command line arguments (if any) will override the default
    page title.
- `custom_template_placeholders` of type object, defines values that will replace custom 
    placeholders when the template is resolved. The values type must be string only.

> __NOTE.__ The placeholders are substituted without checks and modifications that makes it
> possible to inject any code (like JavaScript) into the generated HTML documents
> via the source texts. This must not be a problem for personal use but probably may be a
> security issue when converting and publishing source texts from untrusted third-parties.
> Still such inclusions may be also done with direct HTML code in the source texts (see 
> [here](#html_code_inclision) for more more description and example).
> 


## Templates

A template file (see the source code of [this file](doc_src/templates/default.html) as an 
example) consists of HTML code that is translated as-is and placeholders that are replaced
with their corresponding content. The placeholder format is `${name}`. The following
placeholders are implemented:

- `${title}` --- will be replaced with the page title;
- `${styles}` --- will be replaced with the in-lined or linked CSS;
- `${content}` --- will be replaced with the result of the Markdown document processing;
- `${exec_name}` --- will be replaced with the generator name;
- `${exec_version}` --- will be replaced with the generator version;
- `${generation_date}` --- will be replaced with the generation date (YYYY-MM-DD);
- `${generation_time}` --- will be replaced with the generation time (hh:mm:ss).
- `${custom_key}` --- any custom placeholder keys defined in the page metadata.

> __Notes.__ __1.__ In uncertain cases `$$` may be used to represent a single `$` in a template.
> This does not apply to the Markdown texts where expressions like `${name}` are not
> processed.
> 
> __2.__ Though the Python version will recognize placeholders in format `$name` (without curly
> braces), it's better not to use them for compatibility.

This document is created using a __custom template__ that contains specific elements and so 
cannot be set as the default. If you want to use this template in your project, just make a
copy of corresponding files (the custom template and the additional CSS file) and change them
accordingly.

If no template is specified then the __default template__ is used. This default template was 
developed empirically and must be suitable for most documentation tasks. It also may be used
and specified explicitly as any other template.


## CSS

By default CSS is included into the HTML document; this makes documents standalone (if they don't 
use pictures and other resources). Command line arguments can redefine this behavior.

----------------------------------------------------------------------------------------------------
# Demo

This document itself demonstrates the capabilities of this converter. This section provides
some other examples. See the source Markdown files, like [this](readme.txt), to know how such
results may be obtained.


## Text effects

<div class="tableGridded"></div>

Text effect | Markdown syntax
---|---
**bold** | `__bold__` or `**bold**`
*italic* | `_italic_` or `*italic*`
`in-line code fragment` | `` `in-line code fragment` ``
<del>deleted</del> | `<del>deleted</del>`
<ins>inserted</ins> | `<ins>inserted</ins>`
--- (em-dash, `&mdash;`) | `---`

> __Note.__ Special markdown syntax for inserted (`++inserted++`) and deleted (`~~deleted~~`)
> text is not implemented in this utility. First, these effects must not be used very often.
> Second, such murk-up may cause problems if we write text like "C++", or `++i`.


## Table of contents

`[TOC]` element in the Markdown document will resolve into the document's table of contents in
the generated HTML. The example can be seen at the top of this page.

> Unfortunately by now there's no way to insert a local ToC, i.e. a ToC for a certain header 
> that would contain only the sub-headers of this header.


## Links and images

Link and image usage is demonstrated multiple times in this document. The link syntax is 
`[link text](path/to/doc.html "title")` where `"title"` is not required. `link text` may also
be omitted but the link will be invisible. There's another syntax `<path/to/doc.html>` that will
make the link text equal the link location.

The image syntax is `![ALT](path/to/image.png "title")`, where `ALT` and `"title"` are not
required.

<a name="anchor_demo"></a>

An image may be used as a link text. So the following code 
`[![TARGET](doc/pict/target.png)](readme.html#anchor_demo)` will create the following link:

[![TARGET](doc/pict/target.png)](readme.html#anchor_demo)

> To place an anchor, the following code was used: `<a name="anchor_demo"></a>`.


## Lists

The following text demonstrates a multi-level list.

- This is a __first-level__ _unordered_ list item.
    1. This is a __second-level__ _ordered_ list sub-item.
        - This is a __third-level__ _unordered_ list sub-item.
        - This is another __third-level__ _unordered_ list sub-item.
    1. This is another __second-level__ _ordered_ list sub-item.
- This is another __first-level__ _unordered_ list item.


## Blockquotes

Blockquotes are inserted by starting each line with `> `.

> Links, images and some other Markdown elements may be used inside `blockquote`s: 
> 
> - Here's a link with a clickable image:    
>   [![TARGET](doc/pict/target-smaller.png)](readme.html#anchor_demo)
> - Also a list inside this `blockquote` is use for demonstration.


## Tables

Tables may be used for representing tabular data and for text alignment sometimes. In this
implementation light table style is selected as the default.

Item No | Name | Description | Price
:------:|------|-------------|-----:
1       | Chair | Kitchen chair |  87.50
2       | Table | Kitchen table | 450.00
3       | Lamp  | Standard lamp | 120.75

If we want to just align text we can use a table without a header:

| | | | |
|---:|---: |---: |---: |
| 10 | 20  | 30  | 40  |
| 50 | 60  | 70  | 80  |
| 90 | 100 | 110 | 120 |

> __Note.__ Small extra gap appears above header-less tables that is not avoidable as far now.

Markdown doesn't have syntax for different table styles, but some trick may be used to get 
this --- we can add an invisible element (an empty `<div>` in this case)
right before the table and then use some simple CSS magic to define the appearance:

````code
<div class="tableGridded"></div>
````

Then we can get styles like this:

<div class="tableGridded"></div>

|Item No | Name | Description | Price|
|:------:|------|-------------|-----:|
|1       | Chair | Kitchen chair |  87.50|
|2       | Table | Kitchen table | 450.00|
|3       | Lamp  | Standard lamp | 120.75|

and this (`class="tablePlated"`):

<div class="tablePlated"></div>

Item No | Name | Description | Price
:------:|------|-------------|-----:
1       | Chair | Kitchen chair |  87.50
2       | Table | Kitchen table | 450.00
3       | Lamp  | Standard lamp | 120.75


## Fenced code blocks

Fenced code blocks may be set up by four-space indent, or by four backticks (` ```` `), i.e.
the following mark-up:

    ````
    Example of
        preformatted 
        text
    ````

will look like:

    Example of
        preformatted 
        text

This type of block visually corresponds to `inline` code fragments. 

Style may be specified the following way:

    ````text
    This is a light code block example.
        It is also preformatted
        but has no background.
    ````

The result will be:

````text
This is a light code block example.
    It is also preformatted
    but has no background.
````

There are also shell code block (marked as `shell`):

````shell
$ echo "This block demonstrates operations in a shell"
This block demonstrates operations in a shell
````

and program language code block (marked as `code`):

````code
System.out.println("This block demonstrates " + 
    "a source code in a program language.");
````

----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------
