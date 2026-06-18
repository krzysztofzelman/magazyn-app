package com.example.magazyn.ksef.model.dto;

public record KSeFDashboardStats(
    long totalSent,
    long accepted,
    long rejected,
    long pending,
    long errors,
    long corrected,
    long activeSessions,
    boolean sessionActive,
    String lastSessionNip,
    String lastSessionStatus
) {}
