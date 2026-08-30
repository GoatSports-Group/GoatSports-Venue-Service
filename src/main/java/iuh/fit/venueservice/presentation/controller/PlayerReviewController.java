package iuh.fit.venueservice.presentation.controller;

import iuh.fit.venueservice.application.dto.request.venue.CreatePlayerReviewCommand;
import iuh.fit.venueservice.application.dto.response.venue.PlayerReviewView;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.security.CurrentUserProvider;
import iuh.fit.venueservice.application.service.ReviewService;
import iuh.fit.venueservice.application.util.BaseUtil;
import iuh.fit.venueservice.presentation.request.venue.CreatePlayerReviewRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/player/reviews")
@RequiredArgsConstructor
public class PlayerReviewController {
    private final ReviewService reviewService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_OWN_REVIEW')")
    public ResponseEntity<PlayerReviewView> createReview(
            @Valid @RequestBody CreatePlayerReviewRequest request
    ) throws ServiceException {
        UUID playerId = BaseUtil.uuid(currentUserProvider.getCurrentUserId()
                .orElseThrow(() -> new ServiceException("Bạn phải đăng nhập để gửi đánh giá.")));
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.createPlayerReview(
                new CreatePlayerReviewCommand(
                        playerId, request.bookingId(), request.rating(), request.content()
                )
        ));
    }
}
