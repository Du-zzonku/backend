package com.test.dosa_backend.domain;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class Part {

    @Id
    private String partId;

    @ManyToOne
    @JoinColumn(name = "model_id", nullable = false)
    private Model model;

    @Column(nullable = false)
    private String displayNameKo;

    private String glbUrl;

    private String summary;

}
