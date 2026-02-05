package com.test.dosa_backend.dto;

import com.test.dosa_backend.domain.AssemblyNode;
import lombok.*;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExplodeDto {

    private List<Double> dir;

    private Double distance;

    private Double start;

    private Double duration;

    public static ExplodeDto fromNode(AssemblyNode node) {
        return ExplodeDto.builder()
                .dir(List.of(node.getExplodeDirX(), node.getExplodeDirY(), node.getExplodeDirZ()))
                .distance(node.getExplodeDistance())
                .start(node.getExplodeStart())
                .duration(node.getExplodeDuration())
                .build();
    }


}
