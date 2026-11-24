// src/test/java/com/umc/movie/service/RatingServiceTest.java
package com.umc.movie.service;

import com.umc.movie.dto.CreateRatingRequest;
import com.umc.movie.dto.UpdateRatingRequest;
import com.umc.movie.entity.Member;
import com.umc.movie.entity.Rating;
import com.umc.movie.exception.ApiException;
import com.umc.movie.repository.MemberRepository;
import com.umc.movie.repository.RatingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private RatingService ratingService;

    private final Member member = Member.builder()
            .email("challenger@umc.com")
            .password("암호화된비밀번호")
            .nickname("영화조아")
            .build();

    @Test
    @DisplayName("이미 평점을 남긴 영화면 RATING409_1 예외를 던지고 저장하지 않는다")
    void createRatingWhenDuplicated() {
        // given: 같은 회원이 같은 영화에 이미 평점을 남겨둔 상황
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(ratingRepository.existsByMember_MemberIdAndMovieId(1L, 550L)).willReturn(true);

        // when & then
        assertThatThrownBy(() ->
                ratingService.create(1L, new CreateRatingRequest(550L, 5, "좋아요")))
                .isInstanceOf(ApiException.class)
                .hasMessage("이미 이 영화에 평점을 남기셨습니다.");

        verify(ratingRepository, never()).save(any(Rating.class));
    }

    @Test
    @DisplayName("남의 평점을 수정하려 하면 RATING403_1 예외를 던지고 수정하지 않는다")
    void updateRatingWhenNotOwner() {
        // given: ratingId=1인 평점의 주인은 memberId=1인데, memberId=2가 수정을 시도
        Member owner = Member.builder()
                .email("owner@umc.com")
                .password("암호화된비밀번호")
                .nickname("주인장")
                .build();
        setMemberId(owner, 1L);

        Rating rating = Rating.builder()
                .member(owner)
                .movieId(550L)
                .score(5)
                .comment("좋아요")
                .build();

        given(ratingRepository.findById(1L)).willReturn(Optional.of(rating));

        // when & then: memberId=2(다른 사람)가 수정을 시도하면 403
        assertThatThrownBy(() ->
                ratingService.update(2L, 1L, new UpdateRatingRequest(1, "별로예요")))
                .isInstanceOf(ApiException.class)
                .hasMessage("본인이 작성한 평점만 수정하거나 삭제할 수 있습니다.");

        verify(ratingRepository, never()).save(any(Rating.class));
    }

    // Member.memberId는 @GeneratedValue라 테스트에서 리플렉션으로 직접 채워줍니다
    private void setMemberId(Member member, Long id) {
        try {
            var field = Member.class.getDeclaredField("memberId");
            field.setAccessible(true);
            field.set(member, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}