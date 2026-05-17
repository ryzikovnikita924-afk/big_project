package com.example.dto;

import lombok.*;

/**
 * Универсальное тело ответа. Структура подходит и для успешного ответа и для ошибки
 */
@SuppressWarnings("unused")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UniversalResponse<T> {

    private int code;

    private String message;

    private T data;

    public UniversalResponse(T data) {
        this.code = 0;
        this.message = "SUCCESS";
        this.data = data;
    }

    public UniversalResponse(int code, String message) {
        this.code = code;
        this.message = message;
        this.data = null;
    }
}
