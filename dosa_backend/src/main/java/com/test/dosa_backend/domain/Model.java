package com.test.dosa_backend.domain;

import com.test.dosa_backend.config.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;

/**
 * Model 데이터 저장
 */
@Entity
@Getter
public class Model extends BaseEntity {

    @Id
    private String modelId;

    @Column(nullable = false)
    private String title;

    private String thumbnailUrl;

    private String overview;

    @Column(columnDefinition = "TEXT")
    private String theory;

}
