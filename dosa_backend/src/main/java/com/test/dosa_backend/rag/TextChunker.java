package com.test.dosa_backend.rag;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    private final ObjectMapper mapper = new ObjectMapper();

    public List<TextChunk> chunk(List<PdfTextExtractor.PageText> pages) {
        // Heuristic: 1200 chars chunk, 150 chars overlap
        final int maxChars = 1200;
        final int overlap = 150;

        List<TextChunk> chunks = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        int startPage = -1;
        int endPage = -1;

        for (PdfTextExtractor.PageText p : pages) {
            String t = p.text();
            if (t == null || t.isBlank()) {
                continue;
            }
            if (startPage == -1) startPage = p.pageNumber();
            endPage = p.pageNumber();

            // page delimiter (help the model cite pages)
            String pagePrefix = "\n\n[page " + p.pageNumber() + "]\n";
            if (buf.length() + pagePrefix.length() + t.length() > maxChars && buf.length() > 0) {
                // flush
                chunks.add(buildChunk(buf.toString(), startPage, endPage));

                // overlap
                String tail = tail(buf.toString(), overlap);
                buf.setLength(0);
                buf.append(tail);
                // reset pages: overlap may cross pages; keep conservative range
                startPage = Math.max(1, endPage);
            }

            buf.append(pagePrefix).append(t);
        }

        if (buf.length() > 0) {
            chunks.add(buildChunk(buf.toString(), startPage == -1 ? 1 : startPage, endPage == -1 ? 1 : endPage));
        }

        // assign chunkIndex
        for (int i = 0; i < chunks.size(); i++) {
            chunks.set(i, new TextChunk(i, chunks.get(i).text(), chunks.get(i).metaJson()));
        }

        return chunks;
    }

    private TextChunk buildChunk(String text, int startPage, int endPage) {
        ObjectNode meta = mapper.createObjectNode();
        meta.put("startPage", startPage);
        meta.put("endPage", endPage);
        meta.put("source", "pdf");
        String metaJson = meta.toString();
        return new TextChunk(-1, text.trim(), metaJson);
    }

    private String tail(String s, int n) {
        if (s == null) return "";
        if (s.length() <= n) return s;
        return s.substring(Math.max(0, s.length() - n));
    }

    public record TextChunk(int chunkIndex, String text, String metaJson) {}
}
