package com.tokenphage.api.domain.badge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.tokenphage.api.domain.badge.repository.UserBadgeGrantRepository;
import com.tokenphage.api.domain.badge.repository.projection.BadgeGrantRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BadgeGrantService의 사용 가능 여부 판단 분기를 검증한다.
 * <p>
 * 쿼리 결과 → 판단 결과 매핑과, 등록되지 않은 코드(null)를 거부하는지 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class BadgeGrantServiceTest {

    private static final String USERNAME = "example-user";
    private static final String PRIVATE_CODE = "contributor";
    private static final String PUBLIC_CODE = "gpu";

    @Mock
    private UserBadgeGrantRepository grantRepo;

    private BadgeGrantService sut;

    @BeforeEach
    void setUp() {
        sut = new BadgeGrantService(grantRepo);
    }

    /**
     * 가짜 조회 결과를 만든다.
     */
    private BadgeGrantRow row(boolean granted, String displayName, String lockedMessage) {
        return new BadgeGrantRow() {
            @Override public boolean getGranted() { return granted; }
            @Override public String getDisplayName() { return displayName; }
            @Override public String getLockedMessage() { return lockedMessage; }
        };
    }

    @Nested
    @DisplayName("배지를 쓸 수 있는지 판단")
    class ResolveGrantTest {

        @Test
        @DisplayName("사용가능여부_누구나쓰는배지_허용")
        void 사용가능여부_누구나쓰는배지_허용() {
            // given
            given(grantRepo.findGrant(USERNAME, PUBLIC_CODE))
                    .willReturn(row(true, "GPU Card", null));

            // when
            BadgeGrantResult result = sut.resolveGrant(USERNAME, PUBLIC_CODE);

            // then
            assertThat(result.granted()).isTrue();
            assertThat(result.title()).isNull();
            assertThat(result.message()).isNull();
        }

        @Test
        @DisplayName("사용가능여부_자격필요한배지에자격있음_허용")
        void 사용가능여부_자격필요한배지에자격있음_허용() {
            // given
            given(grantRepo.findGrant(USERNAME, PRIVATE_CODE))
                    .willReturn(row(true, "Contributor", "PR을 보내고 이 배지를 받아보세요"));

            // when
            BadgeGrantResult result = sut.resolveGrant(USERNAME, PRIVATE_CODE);

            // then
            assertThat(result.granted()).isTrue();
        }

        @Test
        @DisplayName("사용가능여부_자격필요한배지에자격없음_거부하고안내문구반환")
        void 사용가능여부_자격필요한배지에자격없음_거부하고안내문구반환() {
            // given
            given(grantRepo.findGrant(USERNAME, PRIVATE_CODE))
                    .willReturn(row(false, "Contributor", "PR을 보내고 이 배지를 받아보세요"));

            // when
            BadgeGrantResult result = sut.resolveGrant(USERNAME, PRIVATE_CODE);

            // then
            assertThat(result.granted()).isFalse();
            assertThat(result.title()).isEqualTo("Contributor");
            assertThat(result.message()).isEqualTo("PR을 보내고 이 배지를 받아보세요");
        }

        @Test
        @DisplayName("사용가능여부_안내문구가없음_문구없이거부")
        void 사용가능여부_안내문구가없음_문구없이거부() {
            // given
            // 안내 문구 미등록 배지. 기본 문구 대체는 렌더러 담당.
            given(grantRepo.findGrant(USERNAME, PRIVATE_CODE))
                    .willReturn(row(false, "Contributor", null));

            // when
            BadgeGrantResult result = sut.resolveGrant(USERNAME, PRIVATE_CODE);

            // then
            assertThat(result.granted()).isFalse();
            assertThat(result.title()).isEqualTo("Contributor");
            assertThat(result.message()).isNull();
        }

        @Test
        @DisplayName("사용가능여부_등록되지않은배지코드_거부")
        void 사용가능여부_등록되지않은배지코드_거부() {
            // given
            // 등록되지 않은 코드는 조회 결과가 없어 null이 온다. 이 경우 거부한다.
            given(grantRepo.findGrant(USERNAME, "ghost")).willReturn(null);

            // when
            BadgeGrantResult result = sut.resolveGrant(USERNAME, "ghost");

            // then
            assertThat(result.granted()).isFalse();
            assertThat(result.title()).isNull();
            assertThat(result.message()).isNull();
        }

        @Test
        @DisplayName("사용가능여부_호출_받은값이바뀌지않고DB조회로전달됨")
        void 사용가능여부_호출_받은값이바뀌지않고DB조회로전달됨() {
            // given
            given(grantRepo.findGrant(anyString(), anyString()))
                    .willReturn(row(true, "GPU Card", null));
            ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

            // when
            sut.resolveGrant(USERNAME, PUBLIC_CODE);

            // then
            then(grantRepo).should().findGrant(usernameCaptor.capture(), codeCaptor.capture());
            assertThat(usernameCaptor.getValue()).isEqualTo(USERNAME);
            assertThat(codeCaptor.getValue()).isEqualTo(PUBLIC_CODE);
        }
    }
}
