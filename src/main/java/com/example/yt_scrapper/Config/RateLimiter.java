package com.example.yt_scrapper.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Component
public class RateLimiter {

    @Value("${transcript.rate.limit.per.minute:10}")
    private int analyzeRateLimit;

    @Value("${transcript.chat.rate.limit.per.minute:30}")
    private int chatRateLimit;

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Instant>> analyzeRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Instant>> chatRequests = new ConcurrentHashMap<>();

    public boolean isAnalyzeAllowed(String clientIp) {
        return isAllowed(clientIp, analyzeRequests, analyzeRateLimit);
    }

    public boolean isChatAllowed(String clientIp) {
        return isAllowed(clientIp, chatRequests, chatRateLimit);
    }

    private boolean isAllowed(String clientIp, ConcurrentHashMap<String, CopyOnWriteArrayList<Instant>> requestMap,
            int limit) {
        Instant oneMinuteAgo = Instant.now().minusSeconds(60);

        requestMap.computeIfAbsent(clientIp, k -> new CopyOnWriteArrayList<>());
        CopyOnWriteArrayList<Instant> timestamps = requestMap.get(clientIp);

        // Clean old entries
        List<Instant> recent = timestamps.stream()
                .filter(t -> t.isAfter(oneMinuteAgo))
                .collect(Collectors.toList());
        timestamps.clear();
        timestamps.addAll(recent);

        if (timestamps.size() >= limit) {
            return false;
        }

        timestamps.add(Instant.now());
        return true;
    }

    /**
     * Periodic cleanup of stale entries (called every 5 minutes internally or via
     * scheduled task)
     */
    public void cleanup() {
        Instant fiveMinutesAgo = Instant.now().minusSeconds(300);
        analyzeRequests.entrySet()
                .removeIf(entry -> entry.getValue().stream().allMatch(t -> t.isBefore(fiveMinutesAgo)));
        chatRequests.entrySet().removeIf(entry -> entry.getValue().stream().allMatch(t -> t.isBefore(fiveMinutesAgo)));
    }
}
