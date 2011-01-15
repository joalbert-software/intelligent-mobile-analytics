package com.ima.web;

import com.ima.security.Authenticator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

public final class AuthenticationInterceptor extends HandlerInterceptorAdapter {
    private final Authenticator authenticator;

    public AuthenticationInterceptor(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(RequestContext.PRINCIPAL,
                authenticator.authenticate(request.getHeader("Authorization")));
        response.setHeader("Cache-Control", "no-store");
        return true;
    }
}
