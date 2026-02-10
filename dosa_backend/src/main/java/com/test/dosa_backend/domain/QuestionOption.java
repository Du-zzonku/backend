package com.test.dosa_backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 객관식의 경우 문항을 저장 (ex. 1개의 객관식 문제에 5개 문항)
 */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "question_option")
public class QuestionOption {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    private Integer optionNo; // 1, 2, 3...

    private String content;   // 보기 내용
}