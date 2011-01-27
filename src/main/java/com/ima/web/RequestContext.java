package com.ima.web;

import com.ima.security.AuthenticationException;
import com.ima.security.Principal;
import javax.servlet.http.HttpServletRequest;

public final class RequestContext {
    public static final String PRINCIPAL = "ima.principal";
    public static final String REQUEST_ID = "ima.requestId";

    private RequestContext() {
    }

    public static Principal principal(HttpServletRequest request) {
        Object principal = request.getAttribute(PRINCIPAL);
        if (!(principal instanceof Principal)) {
            throw new AuthenticationException();
        }
        return (Principal) principal;
    }

    public static String requestId(HttpServletRequest request) {
        Object value = request.getAttribute(REQUEST_ID);
        return value == null ? "unknown" : value.toString();
    }
}
