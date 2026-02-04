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

    public static ExplodeDto fromNode(AssemblyNode node) {
        return ExplodeDto.builder()
                .dir(List.of(node.getExplodeDirX(), node.getExplodeDirY(), node.getExplodeDirZ()))
                .distance(node.getExplodeDistance())
                .build();
    }


}
