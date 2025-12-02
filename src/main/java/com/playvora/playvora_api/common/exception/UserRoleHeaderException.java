package com.playvora.playvora_api.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UserRoleHeaderException extends RuntimeException {
    public UserRoleHeaderException(String message) {
        super(message);
    }
}


