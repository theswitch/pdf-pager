import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.NonSequentialPDFParser;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDPageLabels;
import org.apache.pdfbox.pdmodel.common.PDPageLabelRange;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.*;

import org.json.*;

// Page Labels: see section 12.4.2 of PDF32000:2008
// Document Outline: see section 12.3.3 of PDF32000:2008

class PDFPager {
    public static void main(String args[]) throws IOException {
        // ensure that we have the correct arguments
        if (args.length < 3) {
            System.out.println("Usage: PDFPager <doc.pdf> <conf.json> <out.pdf>");
            System.exit(1);
        }

        // open file objects
        File inFile = new File(args[0]);
        File raFile = File.createTempFile("pdfparser", "");
        RandomAccessFile raBuf = new RandomAccessFile(raFile, "rw");
        FileInputStream jsonFile = new FileInputStream(args[1]);

        // try to create a parser (will fail if e.g. file doesn't exist)...
        NonSequentialPDFParser parser = new NonSequentialPDFParser(inFile, raBuf);
        parser.setLenient(false);
        // ...and try to parse the pdf
        parser.parse();

        // get the underlying PDDocument for the PDF
        PDDocument document = parser.getPDDocument();
        PDDocumentCatalog catalog = document.getDocumentCatalog();

        // create a tokeniser for the conf json
        JSONTokener tokener = new JSONTokener(jsonFile);
        JSONObject conf = new JSONObject(tokener);

        if (conf.has("PageLabels")) {
            System.out.println("setting document page labels");

            JSONArray confLabels = conf.getJSONArray("PageLabels");
            PDPageLabels labels = new PDPageLabels(document);

            for (int i = 0; i < confLabels.length(); i++) {
                // allocate a new PageLabelRange
                PDPageLabelRange range = new PDPageLabelRange();
                // get the config for this range
                JSONObject confLabel = confLabels.getJSONObject(i);

                if (confLabel.has("cover"))
                    range.setPrefix(confLabel.getString("cover"));
                if (confLabel.has("style")) {
                    String style = "";

                    switch (confLabel.getString("style")) {
                    case "r":
                        style = PDPageLabelRange.STYLE_ROMAN_LOWER;
                        break;
                    case "D":
                        style = PDPageLabelRange.STYLE_DECIMAL;
                        break;
                    }

                    range.setStyle(style);
                }
                if (confLabel.has("start"))
                    range.setStart(confLabel.getInt("start"));

                labels.setLabelItem(confLabel.getInt("pageStart") - 1, range);
            }

            catalog.setPageLabels(labels);
        }

        // create document outline
        if (conf.has("Outlines")) {
            System.out.println("setting document outline");

            JSONArray confOutlines = conf.getJSONArray("Outlines");

            // get labels and pages for the document to create destinations
            PDPageLabels labels = catalog.getPageLabels();
            List pages = catalog.getAllPages();

            if (labels == null) {
                // if there are no existing labels, we make a simple one
                labels = new PDPageLabels(document);
                PDPageLabelRange range = new PDPageLabelRange();
                range.setStyle(PDPageLabelRange.STYLE_DECIMAL);
                labels.setLabelItem(0, range);
            }

            // get the mapping of page labels to 0-indices of pages
            Map<String, Integer> pageMapping = labels.getPageIndicesByLabels();

            PDDocumentOutline outlines = new PDDocumentOutline();
            addOutline(outlines, confOutlines, pages, pageMapping);
            outlines.openNode(); // this might not be necessary

            catalog.setDocumentOutline(outlines);
            catalog.setPageMode(PDDocumentCatalog.PAGE_MODE_USE_OUTLINES);
        }

        try {
            document.save(args[2]);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        document.close();
    }

    // recursively parse outlines from the config and add as tree structure
    private static void addOutline(PDOutlineNode parent, JSONArray tree, List pages, Map<String, Integer> pageMapping) {
        for (int i = 0; i < tree.length(); i++) {
            PDOutlineItem item = new PDOutlineItem();
            JSONObject outlineObj = tree.getJSONObject(i);

            item.setTitle(outlineObj.getString("title"));
            if (outlineObj.has("page"))
                item.setDestination((PDPage) pages.get(pageMapping.get(outlineObj.getString("page"))));

            if (outlineObj.has("children"))
                addOutline(item, outlineObj.getJSONArray("children"), pages, pageMapping);

            parent.appendChild(item);
        }
    }
}
