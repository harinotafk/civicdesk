package com.ram.civicdesk.exception;

/** 409 - request clashes with existing data */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
