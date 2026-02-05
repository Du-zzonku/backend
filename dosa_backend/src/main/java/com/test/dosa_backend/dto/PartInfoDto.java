package com.test.dosa_backend.dto;

import com.test.dosa_backend.domain.Part;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartInfoDto {

    private String partId;

    private String displayNameKo;

    private String glbUrl;

    private String summary;

    private String materialType;

    public static PartInfoDto fromPart(Part part) {
        return PartInfoDto.builder()
                .partId(part.getPartId())
                .displayNameKo(part.getDisplayNameKo())
                .glbUrl(part.getGlbUrl())
                .summary(part.getSummary())
                .materialType(part.getMaterialType())
                .build();
    }


}
