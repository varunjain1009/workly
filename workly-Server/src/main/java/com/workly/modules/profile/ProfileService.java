package com.workly.modules.profile;

import lombok.RequiredArgsConstructor;
import com.workly.modules.search.SearchServiceClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final WorkerProfileRepository workerRepository;
    private final SkillSeekerProfileRepository seekerRepository;
    private final SearchServiceClient searchServiceClient;

    public WorkerProfile createOrUpdateWorkerProfile(WorkerProfile profile) {
        log.debug("ProfileService: [ENTER] createOrUpdateWorkerProfile - Mobile: {}", profile.getMobileNumber());
        if (profile.getSkills() != null && !profile.getSkills().isEmpty()) {
            profile.setSkills(searchServiceClient.normalizeSkills(profile.getSkills()));
        }
        WorkerProfile saved = workerRepository.save(profile);
        log.debug("ProfileService: [EXIT] createOrUpdateWorkerProfile - Synced to persistence layer.");
        return saved;
    }

    @Transactional
    public SkillSeekerProfile createOrUpdateSeekerProfile(SkillSeekerProfile profile) {
        return seekerRepository.save(profile);
    }

    @Transactional
    public SkillSeekerProfile getOrCreateSeekerProfile(String mobileNumber) {
        log.debug("ProfileService: [ENTER] getOrCreateSeekerProfile - Checking existence for mobile: {}", mobileNumber);
        return seekerRepository.findByMobileNumber(mobileNumber).orElseGet(() -> {
            log.debug("ProfileService: Profile natively absent, injecting generic base fallback profile.");
            SkillSeekerProfile profile = new SkillSeekerProfile();
            profile.setMobileNumber(mobileNumber);
            profile.setCreatedAt(java.time.LocalDateTime.now());
            return seekerRepository.save(profile);
        });
    }

    public Optional<WorkerProfile> getWorkerProfile(String mobileNumber) {
        java.util.List<WorkerProfile> profiles = workerRepository.findByMobileNumber(mobileNumber);
        if (profiles.isEmpty()) {
            return Optional.empty();
        }
        if (profiles.size() > 1) {
            // Cleanup duplicates: keep the last modified one or just the first one
            // Here we keep the first one and delete others to fix the data
            WorkerProfile kept = profiles.get(0);
            for (int i = 1; i < profiles.size(); i++) {
                workerRepository.delete(profiles.get(i));
            }
            return Optional.of(kept);
        }
        return Optional.of(profiles.get(0));
    }

    public Optional<SkillSeekerProfile> getSeekerProfile(String mobileNumber) {
        return seekerRepository.findByMobileNumber(mobileNumber);
    }

    public void updateLocation(String mobileNumber, double longitude, double latitude) {
        getWorkerProfile(mobileNumber).ifPresent(p -> {
            p.setLastLocation(new double[] { longitude, latitude });
            workerRepository.save(p);
        });
    }

    public void updateAvailability(String mobileNumber, boolean available) {
        getWorkerProfile(mobileNumber).ifPresent(p -> {
            p.setAvailable(available);
            workerRepository.save(p);
        });
    }

    public void updateDeviceToken(String mobileNumber, String token) {
        log.debug("ProfileService: [ENTER] updateDeviceToken - Synchronizing Firebase maps.");
        // Try to find worker first
        Optional<WorkerProfile> worker = getWorkerProfile(mobileNumber);
        if (worker.isPresent()) {
            log.debug("ProfileService: Resolved Token update for standard WorkerProfile entity.");
            WorkerProfile p = worker.get();
            p.setDeviceToken(token);
            workerRepository.save(p);
            return;
        }

        // Else try seeker
        Optional<SkillSeekerProfile> seeker = seekerRepository.findByMobileNumber(mobileNumber);
        if (seeker.isPresent()) {
            log.debug("ProfileService: Resolved Token update for standard SeekerProfile entity.");
            SkillSeekerProfile p = seeker.get();
            p.setDeviceToken(token);
            seekerRepository.save(p);
        }
        log.debug("ProfileService: [EXIT] updateDeviceToken - Processing sequence finished.");
    }
}
