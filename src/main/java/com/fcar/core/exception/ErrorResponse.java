package com.fcar.core.exception;

import lombok.Value;

@Value
public class ErrorResponse {

    String timestamp;
    int status;
    String error;
    String message;
    String path;
}

