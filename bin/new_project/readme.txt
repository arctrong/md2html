<!--VARIABLES {"noPageTitle": true}-->

<p><img src="<!--path pict-->sample_picture.jpg" class="floatRight" /></p>

<!--index Home page -->
This is an automatically generated documentation template.

[TOC]

<p style="clear: both;"></p>


# What does it contain?

This sample documentation contains several simple pages rendered using a typical template.


<!--index usage-->
# How to use it?

The following steps may be done for customization.

- If just one implementation, Python or Java, is going to be used, one of the files 
    `generate_doc_py.bat` or `generate_doc_java.bat` may be deleted.

- Edit this home page, or delete it if it's not required. In case of deletion, also delete it
    from the `documents` section in the file `md2html_args.json`.

- The picture in this page is added just for demonstration. If it's removed then the image
    file must probably be deleted from the folder `doc/pict`.
    
- If required, create your own `doc/favicon.png` image and replace the existing one.
    
- If required, add custom styles to the file `doc/custom.css`.

- Write your own pages using the existing sample pages in the directory `doc_src\sections`
    as examples.

- Look into the file `md2html_args.json`. Particularly, commented GitHub link may be defined and
    uncommented if the writing worked if published there.

- In the directory `doc_src/sections/ref/` delete the pages `references.txt` and `glossary.txt`
    if they are not going to be used. Some cleanup may be done in the argument file
    `md2html_args.json`, but this is not necessary.

- Consider using alternative *color themes*.

Consult the [instructions](https://arctrong.github.io/md2html/readme.html) if any questions.


