package com.test.dosa_backend.dto;

import com.test.dosa_backend.domain.AssemblyNode;
import lombok.*;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoordinatesDto {

    private List<Double> pos;

    private List<Double> quat;

    private List<Double> scale;

    public static CoordinatesDto fromNode(AssemblyNode node) {
        return CoordinatesDto.builder()
                .pos(List.of(node.getPosX(), node.getPosY(), node.getPosZ()))
                .quat(List.of(node.getQuatX(), node.getQuatY(), node.getQuatZ(), node.getQuatW()))
                .scale(List.of(node.getScaleX(), node.getScaleY(), node.getScaleZ()))
                .build();
    }



}
