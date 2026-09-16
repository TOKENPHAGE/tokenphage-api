package com.tokenphage.api.feature.auth.service;

import com.tokenphage.api.exception.AppException;
import com.tokenphage.api.feature.auth.dto.request.VerifyRequest;
import com.tokenphage.api.feature.auth.dto.response.GistFileResponse;
import com.tokenphage.api.feature.auth.dto.response.GistOwnerResponse;
import com.tokenphage.api.feature.auth.dto.response.GistResponse;
import com.tokenphage.api.feature.auth.exception.AuthErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * GistVerificationService.verify 의 분기별 성공/실패 및 경계 동작을 검증하는 테스트.
 *
 * RestClient fluent 체인은 deep-stub(RETURNS_DEEP_STUBS)으로 종단 body() 호출만 stub 한다.
 */
@DisplayName("GistVerificationService - Gist 소유권/내용 검증")
class GistVerificationServiceTest {

    private static final String USERNAME = "octocat";
    private static final String GIST_ID = "abc123";
    private static final String CHALLENGE = "tokenphage-challenge-token";
    private static final String VERIFICATION_FILE = "tokenphage.txt";

    private RestClient gistRestClient;
    private GistVerificationService service;
    private VerifyRequest req;

    @BeforeEach
    void setUp() {
        // RestClient 체인 전체를 deep-stub 으로 생성해 종단 호출만 stub 가능하게 한다
        gistRestClient = mock(RestClient.class, Answers.RETURNS_DEEP_STUBS);
        service = new GistVerificationService(gistRestClient);
        req = new VerifyRequest(USERNAME, GIST_ID);
    }

    /** Gist API 종단 body() 호출이 주어진 응답을 반환하도록 stub 한다. */
    private void stubGistResponse(GistResponse response) {
        given(gistRestClient.get().uri(anyString(), (Object) any()).retrieve().body(GistResponse.class))
            .willReturn(response);
    }

    /** Gist API 종단 body() 호출이 주어진 예외를 던지도록 stub 한다. */
    private void stubGistThrows(Throwable throwable) {
        given(gistRestClient.get().uri(anyString(), (Object) any()).retrieve().body(GistResponse.class))
            .willThrow(throwable);
    }

    /** 공개+소유자일치+검증파일에 challenge 가 담긴 정상 Gist 응답을 만든다. */
    private GistResponse validGist(String ownerLogin, String fileContent) {
        GistOwnerResponse owner = new GistOwnerResponse(-1L, ownerLogin);
        Map<String, GistFileResponse> files =
            Map.of(VERIFICATION_FILE, new GistFileResponse(VERIFICATION_FILE, fileContent));
        return new GistResponse(GIST_ID, Boolean.TRUE, owner, files);
    }

    /** AppException 단언 헬퍼: throw 된 예외의 errorCode 가 기대 코드와 같은지 검증한다. */
    private void assertThrowsWithErrorCode(AuthErrorCode expected) {
        assertThatThrownBy(() -> service.verify(req, CHALLENGE))
            .isInstanceOf(AppException.class)
            .extracting(e -> ((AppException) e).getErrorCode())
            .isEqualTo(expected);
    }

    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @Test
        @DisplayName("공개+소유자일치+파일내용 일치 시 Gist 응답을 반환한다")
        void 검증_정상Gist_응답반환() {
            // given
            GistResponse gist = validGist(USERNAME, CHALLENGE);
            stubGistResponse(gist);

            // when
            GistResponse result = service.verify(req, CHALLENGE);

            // then
            assertThat(result).isSameAs(gist);
        }
    }

    @Nested
    @DisplayName("Gist API 호출 실패")
    class ApiFailure {

        @Test
        @DisplayName("NotFound(404) 시 GIST_NOT_FOUND")
        void 검증_NotFound_GIST_NOT_FOUND() {
            // given
            HttpClientErrorException.NotFound notFound = (HttpClientErrorException.NotFound)
                HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], null);
            stubGistThrows(notFound);

            // when & then
            assertThrowsWithErrorCode(AuthErrorCode.GIST_NOT_FOUND);
        }

        @Test
        @DisplayName("기타 RestClientException 시 GIST_API_UNAVAILABLE")
        void 검증_RestClientException_GIST_API_UNAVAILABLE() {
            // given
            stubGistThrows(new RestClientException("connection refused"));

            // when & then
            assertThrowsWithErrorCode(AuthErrorCode.GIST_API_UNAVAILABLE);
        }
    }

    @Nested
    @DisplayName("응답 형식 오류")
    class Malformed {

        @Test
        @DisplayName("body 가 null 이면 GIST_MALFORMED")
        void 검증_bodyNull_GIST_MALFORMED() {
            // given
            stubGistResponse(null);

            // when & then
            assertThrowsWithErrorCode(AuthErrorCode.GIST_MALFORMED);
        }

        @Test
        @DisplayName("owner 가 null 이면 GIST_MALFORMED")
        void 검증_ownerNull_GIST_MALFORMED() {
            // given
            GistResponse gist = new GistResponse(GIST_ID, Boolean.TRUE, null,
                Map.of(VERIFICATION_FILE, new GistFileResponse(VERIFICATION_FILE, CHALLENGE)));
            stubGistResponse(gist);

            // when & then
            assertThrowsWithErrorCode(AuthErrorCode.GIST_MALFORMED);
        }
    }

    @Nested
    @DisplayName("소유권/공개 여부 검증")
    class OwnerAndVisibility {

        @Test
        @DisplayName("isPublic=false 면 GIST_NOT_PUBLIC")
        void 검증_비공개Gist_GIST_NOT_PUBLIC() {
            // given
            GistResponse gist = new GistResponse(GIST_ID, Boolean.FALSE,
                new GistOwnerResponse(-1L, USERNAME),
                Map.of(VERIFICATION_FILE, new GistFileResponse(VERIFICATION_FILE, CHALLENGE)));
            stubGistResponse(gist);

            // when & then
            assertThrowsWithErrorCode(AuthErrorCode.GIST_NOT_PUBLIC);
        }

        @Test
        @DisplayName("isPublic=null 이면 GIST_NOT_PUBLIC")
        void 검증_isPublicNull_GIST_NOT_PUBLIC() {
            // given
            GistResponse gist = new GistResponse(GIST_ID, null,
                new GistOwnerResponse(-1L, USERNAME),
                Map.of(VERIFICATION_FILE, new GistFileResponse(VERIFICATION_FILE, CHALLENGE)));
            stubGistResponse(gist);

            // when & then
            assertThrowsWithErrorCode(AuthErrorCode.GIST_NOT_PUBLIC);
        }

        @Test
        @DisplayName("owner.login 이 username 과 다르면 OWNER_MISMATCH")
        void 검증_소유자불일치_OWNER_MISMATCH() {
            // given - 전혀 다른 소유자 이름
            GistResponse gist = validGist("someone-else", CHALLENGE);
            stubGistResponse(gist);

            // when & then
            assertThrowsWithErrorCode(AuthErrorCode.OWNER_MISMATCH);
        }

        @Test
        @DisplayName("경계: owner.login 이 대소문자만 다르면 성공한다(equalsIgnoreCase)")
        void 검증_대소문자만다른소유자_응답반환() {
            // given - "octocat" vs "OctoCat"
            GistResponse gist = validGist("OctoCat", CHALLENGE);
            stubGistResponse(gist);

            // when
            GistResponse result = service.verify(req, CHALLENGE);

            // then
            assertThat(result).isSameAs(gist);
        }
    }

    @Nested
    @DisplayName("검증 파일/챌린지 내용 검증")
    class FileContent {

        @Test
        @DisplayName("tokenphage.txt 파일이 없으면 VERIFICATION_FILE_MISSING")
        void 검증_검증파일없음_VERIFICATION_FILE_MISSING() {
            // given - 다른 파일명만 존재
            GistResponse gist = new GistResponse(GIST_ID, Boolean.TRUE,
                new GistOwnerResponse(-1L, USERNAME),
                Map.of("other.txt", new GistFileResponse("other.txt", CHALLENGE)));
            stubGistResponse(gist);

            // when & then
            assertThrowsWithErrorCode(AuthErrorCode.VERIFICATION_FILE_MISSING);
        }

        @Test
        @DisplayName("files 가 null 이면 VERIFICATION_FILE_MISSING")
        void 검증_filesNull_VERIFICATION_FILE_MISSING() {
            // given
            GistResponse gist = new GistResponse(GIST_ID, Boolean.TRUE,
                new GistOwnerResponse(-1L, USERNAME), null);
            stubGistResponse(gist);

            // when & then
            assertThrowsWithErrorCode(AuthErrorCode.VERIFICATION_FILE_MISSING);
        }

        @Test
        @DisplayName("파일 content 가 challenge 와 불일치하면 CHALLENGE_NOT_FOUND_IN_FILE")
        void 검증_챌린지불일치_CHALLENGE_NOT_FOUND_IN_FILE() {
            // given
            GistResponse gist = validGist(USERNAME, "completely-different-value");
            stubGistResponse(gist);

            // when & then
            assertThrowsWithErrorCode(AuthErrorCode.CHALLENGE_NOT_FOUND_IN_FILE);
        }

        @Test
        @DisplayName("파일 content 가 null 이면 CHALLENGE_NOT_FOUND_IN_FILE")
        void 검증_파일내용null_CHALLENGE_NOT_FOUND_IN_FILE() {
            // given
            GistResponse gist = validGist(USERNAME, null);
            stubGistResponse(gist);

            // when & then
            assertThrowsWithErrorCode(AuthErrorCode.CHALLENGE_NOT_FOUND_IN_FILE);
        }

        @Test
        @DisplayName("경계: content 앞뒤 공백은 trim 후 일치하면 성공한다")
        void 검증_앞뒤공백포함내용_trim후일치_응답반환() {
            // given - challenge 양쪽에 공백/개행 포함
            GistResponse gist = validGist(USERNAME, "  \n" + CHALLENGE + "\t  ");
            stubGistResponse(gist);

            // when
            GistResponse result = service.verify(req, CHALLENGE);

            // then
            assertThat(result).isSameAs(gist);
        }
    }
}
