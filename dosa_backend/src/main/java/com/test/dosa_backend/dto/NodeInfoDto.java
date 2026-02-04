package com.test.dosa_backend.dto;

import com.test.dosa_backend.domain.AssemblyNode;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NodeInfoDto {

    private String nodeId;

    private String partId;

    private String parentNodeId;

    private CoordinatesDto assembled;

    private ExplodeDto explode;

    public static NodeInfoDto fromNode(AssemblyNode node) {
        return NodeInfoDto.builder()
                .nodeId(node.getNodeId())
                .partId(node.getPart().getPartId())
                .parentNodeId((node.getParentNodeId() != null) ? node.getParentNodeId().getNodeId() : null)
                .assembled(CoordinatesDto.fromNode(node))
                .explode(ExplodeDto.fromNode(node))
                .build();

    }



}
