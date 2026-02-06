package com.test.dosa_backend.controller;

import com.test.dosa_backend.dto.QuizResponse;
import com.test.dosa_backend.dto.QuizResultResponse;
import com.test.dosa_backend.dto.QuizSubmitRequest;
import com.test.dosa_backend.service.QuizService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Quiz/Answer 데이터 제공", description = "사용자에게 Quiz를 주고 채점 후 정답/해설을 보여주는 API 입니다.")
@RestController
@RequestMapping("/api/models/{modelId}/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    // 문제 조회 API
    // GET /api/models/v4_engine/quiz?count=3&excludedIds=1,2,3
    @GetMapping("/")
    public ResponseEntity<List<QuizResponse>> getRandomQuizzes(
            @PathVariable String modelId,
            @RequestParam(defaultValue = "3") int count,
            @RequestParam(required = false) List<Long> excludedIds
    ) {
        List<QuizResponse> quizzes = quizService.getQuizzes(modelId, count, excludedIds);

        if (quizzes.isEmpty()) {
            return ResponseEntity.noContent().build(); // 204: 풀 문제가 없음
        }

        return ResponseEntity.ok(quizzes);
    }

    // 답안 제출 및 채점 API
    @PostMapping("/answer")
    public ResponseEntity<QuizResultResponse> submitQuiz(
            @RequestBody QuizSubmitRequest request
    ) {
        QuizResultResponse result = quizService.submitQuiz(request);
        return ResponseEntity.ok(result);
    }
}