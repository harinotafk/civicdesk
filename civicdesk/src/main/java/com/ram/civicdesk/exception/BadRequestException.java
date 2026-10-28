package com.ram.civicdesk.exception;

/** 400 - well-formed request asking for something not allowed */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
