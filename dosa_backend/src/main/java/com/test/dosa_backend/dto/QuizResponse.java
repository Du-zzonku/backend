package com.test.dosa_backend.dto;

import com.test.dosa_backend.domain.QuestionType;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

/**
 * 퀴즈를 보내는 Response
 */
@Getter
@Builder
public class QuizResponse {
    private Long questionId;
    private QuestionType type;
    private String question;
    private List<OptionDto> options;

    @Getter
    @Builder
    public static class OptionDto {
        private Integer no;
        private String content;
    }
}