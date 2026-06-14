package com.tokenphage.api.feature.auth.dto.response;

import java.time.Instant;

public record ChallengeResponse(String challenge, Instant expiresAt) {}
