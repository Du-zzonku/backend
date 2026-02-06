package com.test.dosa_backend.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

/**
 * 사용자의 정답을 채점하여 반환
 */
@Getter
@Builder
public class QuizResultResponse {
    private List<QuestionResult> results;

    @Getter
    @Builder
    public static class QuestionResult {
        private Long questionId;
        private boolean isCorrect;
        private Object userSelected;   // 숫자(2) or 문자("피스톤")
        private String correctAnswer;  // 정답 공개
        private String explanation;    // 해설
    }
}