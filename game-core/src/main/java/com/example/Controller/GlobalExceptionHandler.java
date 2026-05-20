package com.example.Controller;

import com.example.dto.UniversalResponse;
import com.example.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<UniversalResponse<Object>> handleBaseException(BaseException ex) {

        UniversalResponse<Object> response = new UniversalResponse<>(ex.getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpCode()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<UniversalResponse<Object>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        UniversalResponse<Object> response = new UniversalResponse<>(4001, message);
        return ResponseEntity.status(400).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<UniversalResponse<Object>> handleGeneralException(Exception ex) {

        UniversalResponse<Object> response = new UniversalResponse<>(5000, "Internal Server Error: " + ex.getMessage());
        return ResponseEntity.status(500).body(response);
    }
}
