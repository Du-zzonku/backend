package com.test.dosa_backend.rag;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfTextExtractor {

    public List<PageText> extract(String pdfPath) throws IOException {
        File f = new File(pdfPath);
        if (!f.exists()) {
            throw new IllegalArgumentException("PDF not found: " + pdfPath);
        }

        try (PDDocument doc = Loader.loadPDF(f)) {
            int pages = doc.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            List<PageText> out = new ArrayList<>(pages);

            for (int p = 1; p <= pages; p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                String text = stripper.getText(doc);
                text = normalize(text);
                out.add(new PageText(p, text));
            }
            return out;
        }
    }

    private String normalize(String s) {
        if (s == null) return "";
        // Reduce weird whitespace; keep line breaks as single \n
        s = s.replace("\r\n", "\n").replace("\r", "\n");
        // Remove null byte and non-printable control chars that break PostgreSQL UTF-8 inserts.
        s = s.replace("\u0000", "");
        s = s.replaceAll("[\\x00-\\x08\\x0B\\x0E-\\x1F\\x7F]", " ");
        s = s.replaceAll("[\t\f]", " ");
        s = s.replaceAll("[ ]{2,}", " ");
        s = s.replaceAll("\n{3,}", "\n\n");
        return s.trim();
    }

    public record PageText(int pageNumber, String text) {}
}
