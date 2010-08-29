package com.ima.security;

public interface RateLimiter {
    RateDecision acquire(String applicationId, int cost);
}
