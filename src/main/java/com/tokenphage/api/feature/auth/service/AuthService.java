package com.tokenphage.api.feature.auth.service;

import com.tokenphage.api.domain.user.service.UserService;
import com.tokenphage.api.exception.AppException;
import com.tokenphage.api.feature.auth.dto.request.ChallengeRequest;
import com.tokenphage.api.feature.auth.dto.request.VerifyRequest;
import com.tokenphage.api.feature.auth.dto.response.ChallengeResponse;
import com.tokenphage.api.feature.auth.dto.response.GistResponse;
import com.tokenphage.api.feature.auth.dto.response.TokenResponse;
import com.tokenphage.api.feature.auth.exception.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String CHALLENGE_PREFIX = "tknphg_";
    static final String REDIS_KEY_PREFIX = "auth:challenge:";
    /**
     * GitHub 사용자명 정책과 동일한 검증 패턴.
     * - 영문/숫자/하이픈만 허용
     * - 하이픈으로 시작/끝 불가, 하이픈 연속('--') 불가
     * - 1~39자 (첫 글자 1 + 이후 최대 38)
     * 참고: shinnn/github-username-regex
     */
    private static final Pattern USERNAME_REGEX =
            Pattern.compile("[A-Za-z0-9](?:[A-Za-z0-9]|-(?=[A-Za-z0-9])){0,38}");
    private static final SecureRandom RNG = new SecureRandom();

    private final UserService userService;
    private final GistVerificationService gistVerification;
    private final JwtIssuer jwtIssuer;
    private final StringRedisTemplate redis;

    @Value("${auth.gist.challenge-ttl-minutes}")
    private int challengeTtlMinutes;

    /**
     * 일회용 챌린지 토큰을 생성해 Redis에 TTL과 함께 저장하고 반환한다.
     * <p>
     * 클라이언트는 이 토큰을 공개 Gist에 삽입해 소유권을 증명한다.
     * 동일 사용자의 유효한 챌린지가 이미 있으면 새로 발급하지 않고 그 값을 재사용하며 TTL만 갱신한다.
     * 검증 실패 후 재시도할 때마다 챌린지가 바뀌면, 사용자가 Gist를 맞춰놔도 기대값이 달라져
     * 계속 실패하므로 이를 방지한다.
     *
     * @param req 챌린지를 요청한 사용자 정보 (null 불허)
     * @return 챌린지 문자열과 만료 시각을 담은 응답 DTO
     * @throws AppException username 형식이 유효하지 않을 경우 (AUTH_001)
     * @Since 2026-06-08
     */
    public ChallengeResponse createChallenge(ChallengeRequest req) {
        validateUsername(req.username());

        String redisKey = REDIS_KEY_PREFIX + req.username().toLowerCase();
        Duration ttl = Duration.ofMinutes(challengeTtlMinutes);

        // 유효한 챌린지가 남아 있으면 값을 재사용하고 TTL만 갱신한다 (재시도 간 기대값 불변 보장).
        String existing = redis.opsForValue().get(redisKey);
        boolean reused = existing != null;
        String challenge = reused ? existing : generateChallenge();
        redis.opsForValue().set(redisKey, challenge, ttl);

        log.info("Challenge {}: user={}, ttl={} minutes", reused ? "reused" : "created", req.username(), challengeTtlMinutes);
        return new ChallengeResponse(challenge, Instant.now().plus(ttl));
    }

    /**
     * Redis에 보관된 챌린지로 Gist 소유권을 검증하고, 성공 시 사용자 정보를 저장한 뒤 JWT를 발급한다.
     * <p>
     * 검증에 성공하면 일회용 챌린지를 즉시 삭제해 재사용을 막고, Gist 소유자의 GitHub ID/로그인으로
     * 사용자를 저장한 다음 해당 사용자에 대한 JWT를 발급한다.
     *
     * @param req 검증 대상 사용자명(username)과 Gist ID를 담은 요청 (null 불허)
     * @return GitHub ID, 사용자명, 발급된 JWT를 담은 응답 DTO
     * @throws AppException username 또는 gistId가 비어 있는 경우 (AUTH_001),
     *                      유효한 챌린지가 없거나 만료된 경우 (AUTH_002),
     *                      Gist 소유권 검증에 실패한 경우 (AUTH_003 ~ AUTH_009)
     * @Since 2026-06-08
     */
    public TokenResponse verify(VerifyRequest req) {
        validateVerifyInput(req.username(), req.gistId());

        String redisKey = REDIS_KEY_PREFIX + req.username().toLowerCase();
        String challenge = redis.opsForValue().get(redisKey);

        if (challenge == null) {
            log.warn("Challenge expired or missing: user={}", req.username());
            throw new AppException(AuthErrorCode.CHALLENGE_EXPIRED);
        }

        GistResponse gist = gistVerification.verify(req, challenge);
        redis.delete(redisKey);
        userService.saveUser(gist.owner().id(), gist.owner().login());
        String token = jwtIssuer.issue(gist.owner().id(), gist.owner().login());

        log.info("Verification succeeded, JWT issued: user={}", gist.owner().login());
        return new TokenResponse(gist.owner().id(), gist.owner().login(), token);
    }

    private void validateUsername(String username) {
        if (!isValidUsername(username)) {
            log.debug("Invalid username rejected: {}", username);
            throw new AppException(AuthErrorCode.INVALID_USERNAME);
        }
    }

    /**
     * GitHub 사용자명 정책 충족 여부를 반환한다.
     * validateUsername 내부에서만 사용하는 헬퍼이며, 검증 동작은
     * 공개 진입점(createChallenge)을 통해 단위 테스트한다.
     *
     * @param username 검사할 사용자명 (null 허용)
     * @return 정책을 모두 만족하면 true
     */
    private boolean isValidUsername(String username) {
        return username != null
                && !username.isBlank()
                && USERNAME_REGEX.matcher(username).matches();
    }

    private void validateVerifyInput(String username, String gistId) {
        if (username == null || username.isBlank() || gistId == null || gistId.isBlank()) {
            log.debug("Missing required fields: username={}, gistId={}", username, gistId);
            throw new AppException(AuthErrorCode.INVALID_USERNAME);
        }
    }

    private String generateChallenge() {
        byte[] buf = new byte[8];
        RNG.nextBytes(buf);
        return CHALLENGE_PREFIX + HexFormat.of().formatHex(buf);
    }
}
