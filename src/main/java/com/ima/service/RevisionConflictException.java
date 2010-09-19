package com.ima.service;

public final class RevisionConflictException extends RuntimeException {
    public RevisionConflictException() {
        super("resource changed; fetch the latest revision before updating");
    }
}
