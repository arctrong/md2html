<!--VARIABLES {"noPageTitle": true}-->

<p><img src="<!--path pict-->sample_picture.jpg" class="floatRight" /></p>

<!--index Home page -->
This is an automatically generated documentation template.

[TOC]

<p style="clear: both;"></p>


# What does it contain

This project contains several typical pages that may be used as examples.


<!--index usage-->
# How to use

The following steps may be done for customization.

- Delete unnecessary build scripts:
    - For Windows:
        - `generate_doc_py.bat`
        - `generate_doc_java.bat`
    - For Linux and macOS:
        - `generate_doc_py`
        - `generate_doc_java`

- Edit this home page, or delete it if it's not required. In case of deletion, also delete it
    from the `documents` section in the `md2html_args.json` file.

- The picture in this page is added just for demonstration. If it's removed from the page then
    the image file must probably be deleted from the folder `doc/pict`.
    
- If required, create your own `doc/favicon.png` image and replace the existing one.
    
- If required, add custom styles to the file `doc/custom.css`.

- Write your own pages using the existing ones in the directory `doc_src\sections` as examples.

- Look into the file `md2html_args.json`. Particularly, commented GitHub link may be defined and
    uncommented if the writing work if published there.

- In the directory `doc_src/sections/ref/` delete the pages `references.txt` and `glossary.txt`
    if they are not going to be used. Some cleanup may be done in the argument file
    `md2html_args.json`, but this is not necessary.

- Consider using alternative *color themes*.

Consult the [instructions](https://arctrong.github.io/md2html/readme.html) if any questions.


