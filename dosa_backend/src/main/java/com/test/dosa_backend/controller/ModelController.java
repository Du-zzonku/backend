package com.test.dosa_backend.controller;

import com.test.dosa_backend.dto.ModelResponseDto;
import com.test.dosa_backend.service.ModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ModelController {

    private final ModelService modelService;

    @GetMapping("/api/models/{id}/viewer")
    public ResponseEntity<ModelResponseDto> getModelInformation(@PathVariable String id){
        ModelResponseDto modelDetail = modelService.getModelDetail(id);
        return ResponseEntity.ok(modelDetail);
    }

}
