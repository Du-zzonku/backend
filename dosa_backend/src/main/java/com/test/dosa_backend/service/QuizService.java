package com.test.dosa_backend.service;

import com.test.dosa_backend.domain.Question;
import com.test.dosa_backend.domain.QuestionType;
import com.test.dosa_backend.dto.QuizResponse;
import com.test.dosa_backend.dto.QuizResultResponse;
import com.test.dosa_backend.dto.QuizSubmitRequest;
import com.test.dosa_backend.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuestionRepository questionRepository;

    // 1. 문제 출제 (정답 숨김 처리)
    @Transactional(readOnly = true)
    public List<QuizResponse> getQuizzes(String modelId, int count, List<Long> excludedIds) {
        // 빈 리스트 방어 로직 (SQL 에러 방지용 더미 값)
        if (excludedIds == null || excludedIds.isEmpty()) {
            excludedIds = List.of(-1L);
        }

        List<Question> questions = questionRepository.findRandomQuestions(modelId, excludedIds, count);

        return questions.stream().map(q -> {
            // 보기 변환 (주관식은 빈 리스트)
            List<QuizResponse.OptionDto> options = q.getOptions().stream()
                    .map(o -> QuizResponse.OptionDto.builder()
                            .no(o.getOptionNo())
                            .content(o.getContent())
                            .build())
                    .collect(Collectors.toList());

            return QuizResponse.builder()
                    .questionId(q.getId())
                    .type(q.getType())
                    .question(q.getContent())
                    .options(options)
                    .build();
        }).collect(Collectors.toList());
    }

    // 2. 채점 로직
    @Transactional(readOnly = true)
    public QuizResultResponse submitQuiz(QuizSubmitRequest request) {
        List<QuizResultResponse.QuestionResult> results = new ArrayList<>();

        for (QuizSubmitRequest.AnswerItem item : request.getAnswers()) {
            Question question = questionRepository.findById(item.getQuestionId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문제 ID"));

            boolean isCorrect;
            Object userSelected;
            String correctAnswerText;

            if (question.getType() == QuestionType.MULTIPLE_CHOICE) {
                // [객관식]
                int userAns = item.getSelectedOptionNo() != null ? item.getSelectedOptionNo() : 0;
                isCorrect = (question.getCorrectOptionNo() == userAns);
                
                userSelected = userAns;
                correctAnswerText = String.valueOf(question.getCorrectOptionNo());
            } else {
                // [주관식]
                String userAns = item.getSubjectiveAnswer() != null ? item.getSubjectiveAnswer().trim() : "";
                String realAns = question.getCorrectShortAnswer();
                
                // 공백 제거 및 대소문자 무시 비교
                isCorrect = realAns.equalsIgnoreCase(userAns);
                
                userSelected = userAns;
                correctAnswerText = realAns;
            }

            results.add(QuizResultResponse.QuestionResult.builder()
                    .questionId(question.getId())
                    .isCorrect(isCorrect)
                    .userSelected(userSelected) // 숫자 or 문자
                    .correctAnswer(correctAnswerText)
                    .explanation(question.getExplanation())
                    .build());
        }

        return QuizResultResponse.builder()
                .results(results)
                .build();
    }
}