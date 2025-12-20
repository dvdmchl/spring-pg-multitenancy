package org.dreamabout.sw.multitenancy.core;

import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-local context for storing and retrieving multitenancy data.
 */
@UtilityClass
public class MultitenancyThreadContext {

    private static final Map<Long, Map<String, Object>> STORAGE = new ConcurrentHashMap<>();

    private static void init() {
        STORAGE.computeIfAbsent(Thread.currentThread().threadId(), id -> new ConcurrentHashMap<>());
    }

    public static void set(String key, Object value) {
        init();
        STORAGE.get(Thread.currentThread().threadId()).put(key, value);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        Map<String, Object> ctx = STORAGE.get(Thread.currentThread().threadId());
        return ctx != null ? (T) ctx.get(key) : null;
    }

    public static void clear() {
        STORAGE.remove(Thread.currentThread().threadId());
    }

    public static String getCurrentSearchPath() {
        return get("currentSearchPath");
    }

    public static void setCurrentSearchPath(String searchPath) {
        set("currentSearchPath", searchPath);
    }

    public static void setTenantIdentifier(TenantIdentifier tenantIdentifier) {
        set("tenantIdentifier", tenantIdentifier);
    }

    public static TenantIdentifier getTenantIdentifier() {
        return get("tenantIdentifier");
    }
}
