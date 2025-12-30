package com.workly.modules.review;

import com.workly.core.ApiResponse;
import com.workly.modules.review.dto.ReviewDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ApiResponse<ReviewDTO> submitReview(@RequestBody ReviewDTO reviewDto) {
        Review review = new Review();
        review.setJobId(reviewDto.getJobId());
        review.setRating(reviewDto.getRating());
        review.setComment(reviewDto.getComment());

        Review savedReview = reviewService.submitReview(review);
        return ApiResponse.success(toDto(savedReview), "Review submitted successfully");
    }

    @GetMapping("/worker/{mobileNumber}")
    public ApiResponse<List<ReviewDTO>> getWorkerReviews(@PathVariable String mobileNumber) {
        List<Review> reviews = reviewService.getWorkerReviews(mobileNumber);
        return ApiResponse.success(reviews.stream().map(this::toDto).toList(), "Reviews retrieved");
    }

    @GetMapping("/worker/{mobileNumber}/average")
    public ApiResponse<Double> getWorkerAverageRating(@PathVariable String mobileNumber) {
        return ApiResponse.success(reviewService.getAverageRating(mobileNumber), "Average rating retrieved");
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
}
