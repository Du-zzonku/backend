package com.test.dosa_backend.controller;

import com.test.dosa_backend.openai.OpenAiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(payload("BAD_REQUEST", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        String msg = "Validation failed.";
        if (e.getBindingResult() != null && !e.getBindingResult().getFieldErrors().isEmpty()) {
            var fe = e.getBindingResult().getFieldErrors().get(0);
            msg = fe.getField() + ": " + fe.getDefaultMessage();
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(payload("VALIDATION_ERROR", msg));
    }

    @ExceptionHandler(OpenAiException.class)
    public ResponseEntity<Map<String, Object>> handleOpenAi(OpenAiException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(payload("OPENAI_ERROR", e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUpload(MaxUploadSizeExceededException e) {
        String msg = "Uploaded file is too large. Increase upload limits or upload a smaller file.";
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(payload("MAX_UPLOAD_SIZE_EXCEEDED", msg));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, Object>> handleMultipart(MultipartException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(payload("MULTIPART_ERROR", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(payload("INTERNAL_ERROR", e.getMessage()));
    }

    private Map<String, Object> payload(String code, String message) {
        Map<String, Object> m = new HashMap<>();
        m.put("timestamp", Instant.now().toString());
        m.put("code", code);
        m.put("message", message);
        return m;
    }
}
