package com.ram.civicdesk.exception;

/** 401 - not logged in, bad credentials, or account disabled */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
