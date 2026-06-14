package com.tokenphage.api.feature.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GistOwnerResponse(Long id, String login) {}
