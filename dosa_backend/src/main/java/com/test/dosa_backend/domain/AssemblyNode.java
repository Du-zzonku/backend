package com.test.dosa_backend.domain;

import com.test.dosa_backend.config.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

/**
 * Model의 부품 위치(x, y, z), 방향(x, y, z, w), explode 값을 저장
 * 같은 부품이여도 위치나 방향정보가 다를 수 있음
 */
@Entity
@Getter
public class AssemblyNode extends BaseEntity {

    @Id
    private String nodeId;

    @ManyToOne
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;

    @ManyToOne
    @JoinColumn(name = "model_id", nullable = false)
    private Model model;

    @ManyToOne
    @JoinColumn(name = "parent_node_id")
    private AssemblyNode parentNodeId;

    @Column(nullable = false)
    private Double posX;

    @Column(nullable = false)
    private Double posY;

    @Column(nullable = false)
    private Double posZ;

    @Column(nullable = false)
    private Double quatX;

    @Column(nullable = false)
    private Double quatY;

    @Column(nullable = false)
    private Double quatZ;

    @Column(nullable = false)
    private Double quatW;

    @Column(nullable = false)
    private Double scaleX;

    @Column(nullable = false)
    private Double scaleY;

    @Column(nullable = false)
    private Double scaleZ;

    @Column(nullable = false)
    private Double explodeDirX;

    @Column(nullable = false)
    private Double explodeDirY;

    @Column(nullable = false)
    private Double explodeDirZ;

    @Column(nullable = false)
    private Double explodeDistance;

    @Column(nullable = false)
    private Double explodeStart;

    @Column(nullable = false)
    private Double explodeDuration;

}
