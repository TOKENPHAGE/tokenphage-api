package com.tokenphage.api.feature.auth.service;

import com.tokenphage.api.exception.AppException;
import com.tokenphage.api.feature.auth.dto.request.VerifyRequest;
import com.tokenphage.api.feature.auth.dto.response.GistFileResponse;
import com.tokenphage.api.feature.auth.dto.response.GistResponse;
import com.tokenphage.api.feature.auth.exception.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GistVerificationService {

    public static final String VERIFICATION_FILE = "tokenphage.txt";

    private final RestClient gistRestClient;

    /**
     * 주어진 Gist가 존재하고 공개 상태인지, req 소유인지,
     * 검증 파일이 있고 expectedChallenge와 일치하는지 순서대로 검증한다.
     *
     * @param req               검증 요청 DTO (username, gistId 포함)
     * @param expectedChallenge Redis에 저장된 챌린지 문자열
     * @return 검증 성공 시 Gist 응답 객체
     * @throws AppException 검증 실패 시 (AuthErrorCode 참조)
     * @Since 2026-05-25
     */
    public GistResponse verify(VerifyRequest req, String expectedChallenge) {
        GistResponse gist;
        try {
            gist = gistRestClient.get()
                    .uri("/gists/{id}", req.gistId())
                    .retrieve()
                    .body(GistResponse.class);

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Gist not found: gistId={}", req.gistId());
            throw new AppException(AuthErrorCode.GIST_NOT_FOUND);
        } catch (RestClientException e) {
            log.error("Gist API call failed: {}", e.getMessage());
            throw new AppException(AuthErrorCode.GIST_API_UNAVAILABLE);
        }

        if (gist == null || gist.owner() == null) {
            log.warn("Gist response malformed: gistId={}", req.gistId());
            throw new AppException(AuthErrorCode.GIST_MALFORMED);
        }

        validateGistOwnerAndFile(gist, req.username(), expectedChallenge);
        log.info("Gist verification passed: user={}, gistId={}", req.username(), req.gistId());
        return gist;
    }

    /**
     * 공개 여부, 소유자 일치, 파일 내용 순으로 검증한다. 실패 시 AppException을 던진다.
     */
    private void validateGistOwnerAndFile(GistResponse gist, String claimedUsername, String expectedChallenge) {
        if (!Boolean.TRUE.equals(gist.isPublic())) {
            throw new AppException(AuthErrorCode.GIST_NOT_PUBLIC);
        }
        if (!claimedUsername.equalsIgnoreCase(gist.owner().login())) {
            throw new AppException(AuthErrorCode.OWNER_MISMATCH);
        }
        validateFileContent(gist.files(), expectedChallenge);
    }

    /**
     * 검증 파일 존재 여부 및 챌린지 일치 여부를 확인한다. 실패 시 AppException을 던진다.
     */
    private void validateFileContent(Map<String, GistFileResponse> files, String expectedChallenge) {
        GistFileResponse file = files == null ? null : files.get(VERIFICATION_FILE);
        if (file == null) {
            throw new AppException(AuthErrorCode.VERIFICATION_FILE_MISSING);
        }
        String content = file.content();
        if (content == null || !content.trim().equals(expectedChallenge)) {
            throw new AppException(AuthErrorCode.CHALLENGE_NOT_FOUND_IN_FILE);
        }
    }
}
