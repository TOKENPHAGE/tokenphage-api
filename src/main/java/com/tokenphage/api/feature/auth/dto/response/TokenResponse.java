package com.tokenphage.api.feature.auth.dto.response;

public record TokenResponse(Long githubId, String username, String token) {}
