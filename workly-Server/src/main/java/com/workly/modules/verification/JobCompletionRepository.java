package com.workly.modules.verification;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface JobCompletionRepository extends JpaRepository<JobCompletion, Long> {
    Optional<JobCompletion> findByJobId(String jobId);
}
