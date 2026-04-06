package com.workly.modules.review;

import com.workly.modules.review.dto.ReviewDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReviewControllerTest {

    @Mock private ReviewService reviewService;

    private ReviewController reviewController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        reviewController = new ReviewController(reviewService);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("m1", null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitReview_success() {
        ReviewDTO dto = new ReviewDTO();
        dto.setJobId("j1");
        dto.setRating(5);
        dto.setComment("Great");

        Review saved = new Review();
        saved.setId("r1");
        saved.setJobId("j1");
        saved.setRating(5);
        when(reviewService.submitReview(any(Review.class), eq("m1"))).thenReturn(saved);

        var result = reviewController.submitReview(dto);

        assertNotNull(result.getData());
        assertEquals("j1", result.getData().getJobId());
        verify(reviewService).submitReview(any(Review.class), eq("m1"));
    }

    @Test
    void getWorkerReviews_returnsList() {
        Review r = new Review(); r.setRating(4);
        when(reviewService.getWorkerReviews("w1")).thenReturn(List.of(r));

        var result = reviewController.getWorkerReviews("w1");

        assertEquals(1, result.getData().size());
    }

    @Test
    void getWorkerAverageRating_returnsAverage() {
        when(reviewService.getAverageRating("w1")).thenReturn(4.5);

        var result = reviewController.getWorkerAverageRating("w1");

        assertEquals(4.5, result.getData());
    }
}
