package com.test.dosa_backend.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ModelSummaryDto {
    private String modelId;
    private String overview;

    public ModelSummaryDto(String modelId, String overview) {
        this.modelId = modelId;
        this.overview = overview;
    }
}
