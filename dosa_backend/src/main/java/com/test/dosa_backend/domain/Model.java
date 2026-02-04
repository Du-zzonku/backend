package com.test.dosa_backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;

@Entity
@Getter
public class Model {

    @Id
    private String modelId;

    @Column(nullable = false)
    private String title;

    private String thumbnailUrl;

    private String overview;

    private String theory;

}
