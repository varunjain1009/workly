package com.workly.modules.review;

import com.workly.core.ApiResponse;
import com.workly.modules.review.dto.ReviewDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ApiResponse<ReviewDTO> submitReview(@RequestBody ReviewDTO reviewDto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();
        log.debug("ReviewController: [ENTER] submitReview - mobile: {}, jobId: {}, rating: {}",
                mobileNumber, reviewDto.getJobId(), reviewDto.getRating());

        Review review = new Review();
        review.setJobId(reviewDto.getJobId());
        review.setRating(reviewDto.getRating());
        review.setComment(reviewDto.getComment());

        Review savedReview = reviewService.submitReview(review, mobileNumber);
        log.debug("ReviewController: [EXIT] submitReview - reviewId: {}", savedReview.getId());
        return ApiResponse.success(toDto(savedReview), "Review submitted successfully");
    }

    @GetMapping("/worker/{mobileNumber}")
    public ApiResponse<List<ReviewDTO>> getWorkerReviews(@PathVariable String mobileNumber) {
        log.debug("ReviewController: [ENTER] getWorkerReviews - mobile: {}", mobileNumber);
        List<Review> reviews = reviewService.getWorkerReviews(mobileNumber);
        log.debug("ReviewController: [EXIT] getWorkerReviews - found {} reviews for {}", reviews.size(), mobileNumber);
        return ApiResponse.success(reviews.stream().map(this::toDto).toList(), "Reviews retrieved");
    }

    @GetMapping("/worker/{mobileNumber}/average")
    public ApiResponse<Double> getWorkerAverageRating(@PathVariable String mobileNumber) {
        log.debug("ReviewController: [ENTER] getWorkerAverageRating - mobile: {}", mobileNumber);
        Double avg = reviewService.getAverageRating(mobileNumber);
        log.debug("ReviewController: [EXIT] getWorkerAverageRating - avg: {} for {}", avg, mobileNumber);
        return ApiResponse.success(avg, "Average rating retrieved");
    }

    private ReviewDTO toDto(Review review) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(review.getId());
        dto.setJobId(review.getJobId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        if (review.getCreatedAt() != null) {
            dto.setCreatedAt(review.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        return dto;
    }

    @PutMapping("/{reviewId}/dispute")
    public ApiResponse<ReviewDTO> disputeReview(@PathVariable String reviewId, @RequestBody java.util.Map<String, String> payload) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();
        String reason = payload.getOrDefault("reason", "No reason provided");
        log.debug("ReviewController: [ENTER] disputeReview - reviewId: {}, reason: {}", reviewId, reason);
        Review review = reviewService.disputeReview(reviewId, reason, mobileNumber);
        return ApiResponse.success(toDto(review), "Review successfully disputed");
    }
}
