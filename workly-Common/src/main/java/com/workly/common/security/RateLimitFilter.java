package com.workly.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Token-bucket rate limiter using Redis.
 * <p>
 * Read endpoints (GET):  60 requests per minute per user
 * Write endpoints (POST/PUT/PATCH/DELETE): 20 requests per minute per user
 * Unauthenticated endpoints: 30 requests per minute per IP
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;

    private static final String RATE_LIMIT_PREFIX = "ratelimit:";
    private static final int READ_LIMIT = 60;
    private static final int WRITE_LIMIT = 20;
    private static final int ANON_LIMIT = 30;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String method = request.getMethod();
        boolean isRead = "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
        int limit = isRead ? READ_LIMIT : WRITE_LIMIT;

        // Determine the rate-limit key
        String key;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            key = RATE_LIMIT_PREFIX + auth.getName() + ":" + (isRead ? "read" : "write");
        } else {
            key = RATE_LIMIT_PREFIX + "ip:" + getClientIp(request);
            limit = ANON_LIMIT;
        }

        // Increment and check
        Long currentCount = redisTemplate.opsForValue().increment(key);
        if (currentCount != null && currentCount == 1) {
            // First request in this window — set TTL
            redisTemplate.expire(key, WINDOW);
        }

        if (currentCount != null && currentCount > limit) {
            log.warn("RateLimitFilter: Rate limit exceeded for key: {} (count: {}, limit: {})", key, currentCount, limit);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Rate limit exceeded. Try again later.\"}");
            return;
        }

        // Add rate-limit headers for client visibility
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - (currentCount != null ? currentCount : 0))));

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
