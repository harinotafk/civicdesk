package com.ram.civicdesk.exception;

/** 403 - logged in but not allowed to do this */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
