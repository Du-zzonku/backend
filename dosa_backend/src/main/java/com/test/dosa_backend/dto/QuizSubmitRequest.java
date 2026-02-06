package com.test.dosa_backend.dto;

import com.test.dosa_backend.domain.QuestionType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 사용자의 퀴즈 정답을 받는 Request
 */
@Getter
@NoArgsConstructor
public class QuizSubmitRequest {
    private List<AnswerItem> answers;

    @Getter
    @NoArgsConstructor
    public static class AnswerItem {
        private Long questionId;
        private QuestionType type;
        private Integer selectedOptionNo; // 객관식 선택
        private String subjectiveAnswer;  // 주관식 입력
    }
}