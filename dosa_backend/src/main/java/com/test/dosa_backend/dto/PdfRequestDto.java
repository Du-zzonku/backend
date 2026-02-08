package com.test.dosa_backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PdfRequestDto {

    private String modelImage;

    private String memo;

    private List<ChatMessage> chatLogs;

    private List<QuizSet> quizs;


}
