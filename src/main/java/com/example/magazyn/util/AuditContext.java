package com.example.magazyn.util;

/**
 * Thread-local holder for the client IP address resolved by AuditLogFilter.
 * Allows AuditLogService to read the IP without coupling to HttpServletRequest.
 */
public final class AuditContext {

    private static final ThreadLocal<String> IP_HOLDER = new ThreadLocal<>();

    private AuditContext() {
    }

    public static void setIp(String ip) {
        IP_HOLDER.set(ip);
    }

    public static String getIp() {
        return IP_HOLDER.get();
    }

    public static void clear() {
        IP_HOLDER.remove();
    }
}
