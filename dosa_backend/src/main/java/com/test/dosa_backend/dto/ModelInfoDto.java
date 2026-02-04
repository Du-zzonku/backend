package com.test.dosa_backend.dto;

import com.test.dosa_backend.domain.Model;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ModelInfoDto {

    private String modelId;

    private String title;

    private String thumbnailUrl;

    private String overview;

    private String theory;

    public static ModelInfoDto fromModel(Model model) {
        return ModelInfoDto.builder()
                .modelId(model.getModelId())
                .title(model.getTitle())
                .thumbnailUrl(model.getThumbnailUrl())
                .overview(model.getOverview())
                .theory(model.getTheory())
                .build();
    }

}
