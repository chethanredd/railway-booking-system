package com.railway.booking.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final long WINDOW_SECONDS = 60;
    private static final int SEARCH_LIMIT = 60;
    private static final int STATUS_LIMIT = 60;
    private static final int PNR_LIMIT = 30;

    private final Map<String, CounterWindow> counters = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        int limit = resolveLimit(path);
        if (limit <= 0) {
            return true;
        }

        String clientIp = resolveClientIp(request);
        String bucketKey = clientIp + "|" + path;
        long now = Instant.now().getEpochSecond();

        CounterWindow window = counters.compute(bucketKey, (k, current) -> {
            if (current == null || now - current.windowStartEpochSecond >= WINDOW_SECONDS) {
                return new CounterWindow(now, 1);
            }
            return new CounterWindow(current.windowStartEpochSecond, current.count + 1);
        });

        if (window.count > limit) {
            response.setStatus(429);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Too many requests. Please try again in a minute.");
            return false;
        }

        return true;
    }

    private int resolveLimit(String path) {
        if (path == null) {
            return 0;
        }
        if (path.startsWith("/trains/search")) {
            return SEARCH_LIMIT;
        }
        if (path.startsWith("/trains/status")) {
            return STATUS_LIMIT;
        }
        if (path.startsWith("/booking/pnr")) {
            return PNR_LIMIT;
        }
        return 0;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int comma = forwardedFor.indexOf(',');
            return comma > 0 ? forwardedFor.substring(0, comma).trim() : forwardedFor.trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private record CounterWindow(long windowStartEpochSecond, int count) {}
}
