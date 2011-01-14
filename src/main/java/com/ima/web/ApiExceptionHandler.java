package com.ima.web;

import com.ima.security.AccessDeniedException;
import com.ima.security.AuthenticationException;
import com.ima.service.QueryTooLargeException;
import com.ima.service.RateLimitException;
import com.ima.store.StoreUnavailableException;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public final class ApiExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({com.ima.service.RevisionConflictException.class, javax.persistence.OptimisticLockException.class})
    public ResponseEntity<ApiError> revisionConflict(Exception failure, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "revision_conflict", "resource changed; fetch the latest revision", request);
    }

    @ExceptionHandler(com.ima.service.ResourceNotFoundException.class)
    public ResponseEntity<ApiError> missing(com.ima.service.ResourceNotFoundException failure,
                                            HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "not_found", failure.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> authentication(AuthenticationException failure, HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("WWW-Authenticate", "Bearer");
        return new ResponseEntity<ApiError>(new ApiError("unauthorized", failure.getMessage(),
                RequestContext.requestId(request)), headers, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> forbidden(AccessDeniedException failure, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "forbidden", "permission denied", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> invalid(IllegalArgumentException failure, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "invalid_input", failure.getMessage(), request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> malformed(Exception failure, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "invalid_request", "request fields are missing or malformed", request);
    }

    @ExceptionHandler(QueryTooLargeException.class)
    public ResponseEntity<ApiError> tooLarge(QueryTooLargeException failure, HttpServletRequest request) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "query_too_large", failure.getMessage(), request);
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ApiError> limited(RateLimitException failure, HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", Long.toString(Math.max(1, (failure.getRetryAfterMillis() + 999) / 1000)));
        return new ResponseEntity<ApiError>(new ApiError("rate_limited", failure.getMessage(),
                RequestContext.requestId(request)), headers, HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(StoreUnavailableException.class)
    public ResponseEntity<ApiError> unavailable(StoreUnavailableException failure, HttpServletRequest request) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "storage_unavailable", "storage is temporarily unavailable", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception failure, HttpServletRequest request) {
        LOG.error("request_id={} failure_type={}", RequestContext.requestId(request), failure.getClass().getSimpleName());
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "request could not be completed", request);
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String code, String message,
                                               HttpServletRequest request) {
        return new ResponseEntity<ApiError>(new ApiError(code, message, RequestContext.requestId(request)), status);
    }
}
