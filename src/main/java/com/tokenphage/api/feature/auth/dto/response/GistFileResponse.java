package com.tokenphage.api.feature.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GistFileResponse(String filename, String content) {}
