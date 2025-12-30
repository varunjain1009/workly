package com.workly.modules.review;

import com.workly.core.WorklyException;
import com.workly.modules.job.Job;
import com.workly.modules.job.JobStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final com.workly.modules.job.JobService jobService;

    public Review submitReview(Review review) {
        Job job = jobService.getJobById(review.getJobId());

        if (job.getStatus() != JobStatus.COMPLETED) {
            throw WorklyException.badRequest("Reviews can only be submitted for completed jobs");
        }

        if (reviewRepository.findByJobId(review.getJobId()).isPresent()) {
            throw WorklyException.badRequest("A review already exists for this job");
        }

        review.setSeekerMobileNumber(job.getSeekerMobileNumber());
        review.setWorkerMobileNumber(job.getWorkerMobileNumber());

        return reviewRepository.save(review);
    }

    public List<Review> getWorkerReviews(String mobileNumber) {
        return reviewRepository.findByWorkerMobileNumber(mobileNumber);
    }

    public double getAverageRating(String mobileNumber) {
        List<Review> reviews = getWorkerReviews(mobileNumber);
        if (reviews.isEmpty())
            return 0.0;
        return reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
    }
}
