package com.test.dosa_backend.controller;

import com.test.dosa_backend.dto.QuizResponse;
import com.test.dosa_backend.dto.QuizResultResponse;
import com.test.dosa_backend.dto.QuizSubmitRequest;
import com.test.dosa_backend.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Quiz API", description = "퀴즈 조회, 제출 및 채점과 관련된 API를 제공합니다.")
@RestController
@RequestMapping("/api/models/{modelId}/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @Operation(
            summary = "랜덤 퀴즈 조회",
            description = "지정된 모델 ID에 해당하는 퀴즈를 랜덤으로 조회합니다. 이미 푼 문제는 excludedIds로 제외할 수 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "퀴즈 조회 성공", content = @Content(schema = @Schema(implementation = QuizResponse.class))),
            @ApiResponse(responseCode = "204", description = "풀 수 있는 문제가 없음 (조회 결과 0건)", content = @Content),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터", content = @Content)
    })
    @GetMapping("/")
    public ResponseEntity<List<QuizResponse>> getRandomQuizzes(
            @Parameter(description = "모델 식별자 (예: v4_engine)", example = "v4_engine", required = true)
            @PathVariable String modelId,

            @Parameter(description = "한 번에 가져올 퀴즈 개수", example = "3")
            @RequestParam(defaultValue = "3") int count,

            @Parameter(description = "제외할 퀴즈 ID 리스트 (예: 1,2,3 - 이미 풀었던 문제)", example = "[1, 2, 3]")
            @RequestParam(required = false) List<Long> excludedIds
    ) {
        List<QuizResponse> quizzes = quizService.getQuizzes(modelId, count, excludedIds);

        if (quizzes.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(quizzes);
    }

    @Operation(
            summary = "퀴즈 답안 제출 및 채점",
            description = "사용자가 푼 퀴즈의 답안을 제출하고, 정답 여부와 해설을 반환받습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "채점 완료 및 결과 반환", content = @Content(schema = @Schema(implementation = QuizResultResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 퀴즈를 찾을 수 없음", content = @Content)
    })
    @PostMapping("/answer")
    public ResponseEntity<QuizResultResponse> submitQuiz(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "제출할 답안 정보", required = true)
            @RequestBody QuizSubmitRequest request
    ) {
        QuizResultResponse result = quizService.submitQuiz(request);
        return ResponseEntity.ok(result);
    }
}