package com.test.dosa_backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ModelResponseDto {

    private ModelInfoDto model;

    private List<PartInfoDto> parts;

    private List<NodeInfoDto> nodes;

    public static ModelResponseDto of(ModelInfoDto modelInfoDto, List<PartInfoDto> parts, List<NodeInfoDto> nodes) {
        return ModelResponseDto.builder()
                .model(modelInfoDto)
                .parts(parts)
                .nodes(nodes)
                .build();
    }

}
