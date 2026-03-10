package com.workly.modules.admin;

import com.workly.modules.job.Job;
import com.workly.modules.job.JobRepository;
import com.workly.modules.profile.SkillSeekerProfile;
import com.workly.modules.profile.SkillSeekerProfileRepository;
import com.workly.modules.profile.WorkerProfile;
import com.workly.modules.profile.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final SkillSeekerProfileRepository seekerRepository;
    private final WorkerProfileRepository workerRepository;
    private final JobRepository jobRepository;

    public Page<SkillSeekerProfile> getSeekers(Pageable pageable) {
        return seekerRepository.findAll(pageable);
    }

    public Page<WorkerProfile> getProviders(Pageable pageable) {
        return workerRepository.findAll(pageable);
    }

    public Page<Job> getJobs(Pageable pageable) {
        return jobRepository.findAll(pageable);
    }
}
