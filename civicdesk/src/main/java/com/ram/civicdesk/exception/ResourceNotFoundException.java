package com.ram.civicdesk.exception;

/** 404 - something we looked up does not exist (or the caller may not see it) */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
