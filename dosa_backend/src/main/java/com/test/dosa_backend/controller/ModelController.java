package com.test.dosa_backend.controller;

import com.test.dosa_backend.dto.ModelSliceDto;
import com.test.dosa_backend.dto.ModelResponseDto;
import com.test.dosa_backend.service.ModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ModelController {

    private final ModelService modelService;

    @GetMapping("/models/{id}/viewer")
    public ResponseEntity<ModelResponseDto> getModelInformation(@PathVariable String id){
        ModelResponseDto modelDetail = modelService.getModelDetail(id);
        return ResponseEntity.ok(modelDetail);
    }

    @GetMapping("/models")
    public ResponseEntity<ModelSliceDto> getModelIds (
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        ModelSliceDto modelSliceDto = modelService.getModelIds(pageable);
        return ResponseEntity.ok(modelSliceDto);
    }

}
