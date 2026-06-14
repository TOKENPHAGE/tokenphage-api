package com.tokenphage.api.feature.badge.dto.response;

import java.util.List;

public record BadgeResponse(
    String username,
    long totalTokens,
    List<DailyCountResponse> heatbar,
    List<ModelCountResponse> topModels,
    double cacheHitRate
) {}
