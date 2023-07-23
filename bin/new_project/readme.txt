<!--VARIABLES {"noPageTitle": true}-->

<p><img src="<!--path pict-->sample_picture.jpg" class="floatRight" /></p>

<!--index Home page -->
This is an automatically created documentation template.

[TOC]

<p style="clear: both;"></p>


# What does it contain?

This sample documentation contains several simple pages rendered using a typical template.


<!--index usage-->
# How to use it?

The following steps may be done for customization.

- If just one implementation, Python or Java, is going to be used, one of the files 
    `generate_doc_py.bat` or `generate_doc_java.bat` may be deleted.

- Edit this home page, or delete it if it's not required. In case of deletion:

    - also delete it from the `documents` section in the file `md2html_args.json`.

- The picture in this page is added just for demonstration. If it's removed then the image
    file must probably be deleted from the folder `doc/pict`.
    
- If required, create your own `doc/favicon.png` image and replace the existing one.
    
- If required, add custom styles to the file `doc/custom.css`. It's not recommended to modify
    the content of the directory `doc/layout`. If improvements are done in the `md2html`
    program then the directory `doc/layout` may be entirely copied from there with replacement
    while the custom styles will stay untouched.

- Write your own pages using the existing sample pages as examples.

- Look into the file `md2html_args.json`. Particularly "Useful links" section items may be
    redefined there.

- Consider using alternative themes. Have a look in at the variable `"theme": "light_default"`
    in the `md2html_args.json` file. The themes are stored in the `doc/themes` directory.
    Alternative themes may be created from scratch (using the existing ones as examples)
    or taken from the `md2html` program installation directory.
    For example:

    - copy the files `dark_default_content.css` and `dark_default_layout.css` to the
        `doc/themes` directory;
    - assign the value `dark_default` to the variable `theme` in the file `md2html_args.json`;
    - fulfill forcible full documentation regeneration;
    - the dark theme will be applied.

Consult the [instructions](https://arctrong.github.io/md2html/readme.html) if any questions.


