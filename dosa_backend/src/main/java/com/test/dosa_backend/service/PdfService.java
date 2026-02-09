package com.test.dosa_backend.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.test.dosa_backend.dto.PdfRequestDto;
import com.test.dosa_backend.repository.ModelRepository;
import lombok.RequiredArgsConstructor;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class PdfService {

    private final TemplateEngine templateEngine;
    private final ModelRepository modelRepository;

    public byte[] generatePdf(PdfRequestDto requestDto, String id) {
        try {
            // 1. Thymeleaf Context에 데이터 담기
            Context context = new Context();
            context.setVariable("modelImage", requestDto.getModelImage());
            context.setVariable("title", modelRepository.findTitleByModelId(id));
            context.setVariable("overview", modelRepository.findOverviewByModelId(id));
            context.setVariable("theory", markdownToHtml(modelRepository.findTheoryByModelId(id)));
            context.setVariable("memo", requestDto.getMemo());
            context.setVariable("chatLogs", requestDto.getChatLogs());
            context.setVariable("quizs", requestDto.getQuizs());

            // 2. HTML 템플릿을 문자열로 렌더링 (데이터가 채워진 HTML 생성)
            String htmlContent = templateEngine.process("pdf", context);

            // 3. HTML -> PDF 변환
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();

            builder.useFastMode();
            builder.withHtmlContent(htmlContent, "http://localhost/");

            // 한글 폰트 등록
            ClassPathResource fontResource = new ClassPathResource("fonts/NanumGothic.ttf");
            builder.useFont(() -> {
                try {
                    return fontResource.getInputStream();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, "NanumGothic");

            builder.toStream(os);
            builder.run(); // 변환 실행

            return os.toByteArray(); // 완성된 PDF 파일 데이터 반환

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("PDF 생성 중 오류 발생");
        }
    }

    private String markdownToHtml(String markdown) {
        // 1. 문자열 형태의 "\n"을 실제 줄바꿈으로 변경
        String fixed = markdown.replace("\\n", "\n");

        // 2. 리스트(*) 강제 줄바꿈 및 교정
        fixed = fixed.replaceAll("([^\\n])\\s*\\*\\s*\\*\\*", "$1\n\n* **");

        // 3. ** ** 문법 적용
        fixed = fixed.replaceAll("\\*\\*\\s*(.*?)\\s*\\*\\*", " **$1** ");

        Parser parser = Parser.builder().build();
        Node document = parser.parse(fixed);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        return renderer.render(document);
    }

}
