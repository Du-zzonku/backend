package com.test.dosa_backend.controller;

import com.test.dosa_backend.dto.RagDtos;
import com.test.dosa_backend.service.RagService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/rag")
@Tag(name = "RAG", description = "Retrieval-Augmented Generation search")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/search")
    public RagDtos.SearchResponse search(@Valid @RequestBody RagDtos.SearchRequest req) {
        int topK = (req.topK() == null) ? 5 : req.topK();
        var res = ragService.retrieve(req.query(), topK, req.documentIds());
        return new RagDtos.SearchResponse(res.contextText(), res.citations());
    }
}
