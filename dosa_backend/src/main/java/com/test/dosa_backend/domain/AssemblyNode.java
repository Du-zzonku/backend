package com.test.dosa_backend.domain;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class AssemblyNode {

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
