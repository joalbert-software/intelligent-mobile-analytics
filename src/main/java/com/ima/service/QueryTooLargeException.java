package com.ima.service;

public final class QueryTooLargeException extends RuntimeException {
    public QueryTooLargeException() {
        super("query exceeds the online event limit; reduce the range or use batch reporting");
    }
}
