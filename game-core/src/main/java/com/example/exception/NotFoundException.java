package com.example.exception;


public class NotFoundException extends BaseException {

    public static final int HTTP_CODE = 404;

    public static final int CODE = 4000;

    public NotFoundException(String message) {
        super(message);
    }

    @Override
    public int getHttpCode() {
        return HTTP_CODE;
    }

    @Override
    public int getCode() {
        return CODE;
    }
}

