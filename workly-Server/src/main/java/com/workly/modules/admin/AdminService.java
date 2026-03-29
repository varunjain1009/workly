package com.workly.modules.admin;

import com.workly.modules.job.Job;
import com.workly.modules.job.JobRepository;
import com.workly.modules.profile.SkillSeekerProfile;
import com.workly.modules.profile.SkillSeekerProfileRepository;
import com.workly.modules.profile.WorkerProfile;
import com.workly.modules.profile.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final SkillSeekerProfileRepository seekerRepository;
    private final WorkerProfileRepository workerRepository;
    private final JobRepository jobRepository;

    public Page<SkillSeekerProfile> getSeekers(Pageable pageable) {
        log.debug("AdminService: [ENTER] getSeekers - Pagination: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<SkillSeekerProfile> result = seekerRepository.findAll(pageable);
        log.debug("AdminService: [EXIT] getSeekers - Returned {} of {} total seekers", result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    public Page<WorkerProfile> getProviders(Pageable pageable) {
        log.debug("AdminService: [ENTER] getProviders - Pagination: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<WorkerProfile> result = workerRepository.findAll(pageable);
        log.debug("AdminService: [EXIT] getProviders - Returned {} of {} total providers", result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    public Page<Job> getJobs(Pageable pageable) {
        log.debug("AdminService: [ENTER] getJobs - Pagination: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<Job> result = jobRepository.findAll(pageable);
        log.debug("AdminService: [EXIT] getJobs - Returned {} of {} total jobs", result.getNumberOfElements(), result.getTotalElements());
        return result;
    }
}
