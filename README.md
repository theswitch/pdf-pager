pdf-pager
=========

A Java util to add correct page numbering and outlines to PDFs

Dependencies
------------

Requires [Apache PDFBox](https://pdfbox.apache.org) (and therefore [Commons Logging](http://commons.apache.org/proper/commons-logging/)), as well as [JSON in Java](http://json.org/java/). I have no idea what the right way to distribute Java source with libraries is, suffice it to say that these things should be in your classpath somewhere.

Usage
-----
- **Compile**: `javac PDFPager.java`
- **Run**: `java PDFPager <in.pdf> <conf.json> <out.pdf>`

The `conf.json` contains a JSON object with two possible keys: `PageLabels` and `Outlines`. You may have none (in which case the input PDF will be written somewhat uncompressed to the output), just one, or both of these keys. See the example configs for the structure of these; it should be pretty easy to figure out.
