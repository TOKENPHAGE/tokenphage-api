package com.tokenphage.api.feature.badge.svg;

public record BadgeColors(
        String bg, String textPrimary, String textSecondary, String divider,
        String heatLow, String heatMid1, String heatMid2, String heatMid3, String heatHigh) {

    public static final BadgeColors LIGHT = new BadgeColors(
            "#ffffff", "#111827", "#6b7280", "#e5e7eb",
            "#dbeafe", "#93c5fd", "#60a5fa", "#3b82f6", "#1e40af"
    );
    public static final BadgeColors DARK = new BadgeColors(
            "#0f172a", "#ffffff", "#94a3b8", "#1e293b",
            "#1e3a8a", "#1e40af", "#3b82f6", "#60a5fa", "#93c5fd"
    );
}
