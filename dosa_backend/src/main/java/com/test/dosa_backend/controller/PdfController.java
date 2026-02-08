package com.test.dosa_backend.controller;

import com.test.dosa_backend.dto.PdfRequestDto;
import com.test.dosa_backend.service.PdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "PDF 미리보기/다운로드", description = "PDF 미리보기/다운로드 기능을 제공해주는 API입니다.")
public class PdfController {

    private final PdfService pdfService;

    @Operation(summary = "PDF 저장 전 미리보기 및 저장 기능 제공", description = "PDF 저장 전 미리보기 기능과 PDF 저장 기능을 제공합니다.")
    @PostMapping("/models/{id}/pdf")
    public ResponseEntity<byte[]> generatePdf(
            @RequestBody PdfRequestDto requestDto,
            @RequestParam String type,
            @PathVariable String id) {

        // 1. PDF 생성 (서비스 호출)
        byte[] pdfFile = pdfService.generatePdf(requestDto, id);

        // 2. 헤더 설정 (미리보기 vs 다운로드 결정)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);

        if ("preview".equals(type)) {
            // [미리보기 모드] "inline": 브라우저에서 바로 열어라
            headers.setContentDisposition(ContentDisposition.inline().filename("preview.pdf").build());
        } else if("download".equals(type)) {
            // [다운로드 모드] "attachment": 파일로 저장해라
            headers.setContentDisposition(ContentDisposition.attachment().filename("learning_report.pdf").build());
        }

        return new ResponseEntity<>(pdfFile, headers, HttpStatus.OK);
    }

}
