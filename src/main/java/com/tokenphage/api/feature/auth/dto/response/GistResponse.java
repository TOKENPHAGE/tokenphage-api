package com.tokenphage.api.feature.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GistResponse(
    String id,
    @JsonProperty("public") Boolean isPublic,
    GistOwnerResponse owner,
    Map<String, GistFileResponse> files
) {}
