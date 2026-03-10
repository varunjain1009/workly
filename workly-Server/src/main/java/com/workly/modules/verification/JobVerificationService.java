package com.workly.modules.verification;

import com.workly.core.WorklyException;
import com.workly.modules.job.Job;
import com.workly.modules.job.JobRepository;
import com.workly.modules.job.JobStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class JobVerificationService {

    private final JobRepository mongoJobRepository;
    private final JobCompletionRepository jpaJobRepository;

    @Transactional
    public JobCompletion verifyAndCompleteJob(String jobId, String otp) {
        Job mongoJob = mongoJobRepository.findById(jobId)
                .orElseThrow(() -> WorklyException.notFound("Job not found in MongoDB"));

        if (!mongoJob.getCompletionOtp().equals(otp)) {
            throw WorklyException.badRequest("Invalid completion OTP");
        }

        if (mongoJob.getStatus() == JobStatus.COMPLETED) {
            throw WorklyException.badRequest("Job is already completed");
        }

        // 1. Update/Create PostgreSQL state FIRST
        JobCompletion completion = jpaJobRepository.findByJobId(jobId)
                .orElse(new JobCompletion());

        completion.setJobId(jobId);
        completion.setWorkerMobile(mongoJob.getWorkerMobileNumber());
        completion.setSeekerMobile(mongoJob.getSeekerMobileNumber());
        completion.setVerificationOtp(otp);
        completion.setVerified(true);
        completion.setCompletedAt(LocalDateTime.now());

        JobCompletion savedCompletion = jpaJobRepository.save(completion);

        // 2. Update MongoDB state LAST.
        // If this throws, the above JPA save automatically rolls back because of
        // @Transactional.
        // If the JPA save above throws, this MongoDB save is never reached.
        mongoJob.setStatus(JobStatus.COMPLETED);
        mongoJobRepository.save(mongoJob);

        return savedCompletion;
    }
}
