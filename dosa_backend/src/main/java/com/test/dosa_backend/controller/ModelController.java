package com.test.dosa_backend.controller;

import com.test.dosa_backend.dto.ModelResponseDto;
import com.test.dosa_backend.service.ModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Model 정보 제공", description = "Model 정보를 제공해주는 API입니다.")
public class ModelController {

    private final ModelService modelService;

    @Operation(summary = "Model 정보(part, node) 제공", description = "Model에 관한 모든 정보를 제공합니다.")
    @GetMapping("/api/models/{id}/viewer")
    public ResponseEntity<ModelResponseDto> getModelInformation(@PathVariable String id){
        ModelResponseDto modelDetail = modelService.getModelDetail(id);
        return ResponseEntity.ok(modelDetail);
    }

}
