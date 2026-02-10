package com.test.dosa_backend.domain;

import com.test.dosa_backend.config.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

/**
 * Model의 부품을 저장
 */
@Entity
@Getter
public class Part extends BaseEntity {

    @Id
    private String partId;

    @ManyToOne
    @JoinColumn(name = "model_id", nullable = false)
    private Model model;

    @Column(nullable = false)
    private String displayNameKo;

    private String glbUrl;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private String materialType;

}
