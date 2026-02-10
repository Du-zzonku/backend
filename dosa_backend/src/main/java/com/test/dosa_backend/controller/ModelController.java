package com.test.dosa_backend.controller;

import com.test.dosa_backend.dto.ModelSliceDto;
import com.test.dosa_backend.dto.ModelResponseDto;
import com.test.dosa_backend.service.ModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Model API", description = "3D 모델 정보 조회 및 모델 리스트를 제공하는 API입니다.")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ModelController {

    private final ModelService modelService;

    // 1. 모델 상세 정보 조회
    @Operation(
            summary = "모델 상세 정보(Node, Part) 조회",
            description = "특정 모델 ID(id)에 대한 상세 정보(계층 구조, 부품 정보 등)를 반환합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = ModelResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 모델 ID", content = @Content)
    })
    @GetMapping("/api/models/{id}/viewer")
    public ResponseEntity<ModelResponseDto> getModelInformation(
            @Parameter(description = "모델 식별자 (ID)", example = "v4_engine", required = true)
            @PathVariable String id
    ) {
        ModelResponseDto modelDetail = modelService.getModelDetail(id);
        return ResponseEntity.ok(modelDetail);
    }

    // 2. 모델 리스트 조회 (페이징/슬라이스)
    @Operation(
            summary = "모델 목록 조회 (무한 스크롤)",
            description = "등록된 모델들의 목록을 Slice(무한 스크롤) 형태로 조회합니다. 최신순으로 정렬됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "목록 조회 성공", content = @Content(schema = @Schema(implementation = ModelSliceDto.class)))
    })
    @GetMapping("/models")
    public ResponseEntity<ModelSliceDto> getModelIds(
            @Parameter(description = "페이지 정보 (기본값: size=10, sort=createdAt,desc)", example = "{\"page\": 0, \"size\": 10}")
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        ModelSliceDto modelSliceDto = modelService.getModelIds(pageable);
        return ResponseEntity.ok(modelSliceDto);
    }
}