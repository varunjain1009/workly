package com.workly.profilesvc.service;

import com.workly.profilesvc.domain.seeker.SkillSeekerProfile;
import com.workly.profilesvc.domain.seeker.SkillSeekerProfileRepository;
import com.workly.profilesvc.domain.worker.WorkerProfile;
import com.workly.profilesvc.domain.worker.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final WorkerProfileRepository workerRepository;
    private final SkillSeekerProfileRepository seekerRepository;
    private final SearchServiceClient searchServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public WorkerProfile createOrUpdateWorkerProfile(WorkerProfile profile) {
        log.debug("ProfileService: createOrUpdateWorkerProfile - mobile: {}", profile.getMobileNumber());
        if (profile.getSkills() != null && !profile.getSkills().isEmpty()) {
            profile.setSkills(searchServiceClient.normalizeSkills(profile.getSkills()));
        }
        return workerRepository.save(profile);
    }

    public SkillSeekerProfile createOrUpdateSeekerProfile(SkillSeekerProfile profile) {
        return seekerRepository.save(profile);
    }

    public SkillSeekerProfile getOrCreateSeekerProfile(String mobileNumber) {
        return seekerRepository.findByMobileNumber(mobileNumber).orElseGet(() -> {
            SkillSeekerProfile profile = new SkillSeekerProfile();
            profile.setMobileNumber(mobileNumber);
            profile.setCreatedAt(java.time.LocalDateTime.now());
            return seekerRepository.save(profile);
        });
    }

    public Optional<WorkerProfile> getWorkerProfile(String mobileNumber) {
        var profiles = workerRepository.findByMobileNumber(mobileNumber);
        if (profiles.isEmpty()) return Optional.empty();
        if (profiles.size() > 1) {
            WorkerProfile kept = profiles.get(0);
            for (int i = 1; i < profiles.size(); i++) workerRepository.delete(profiles.get(i));
            return Optional.of(kept);
        }
        return Optional.of(profiles.get(0));
    }

    public Optional<SkillSeekerProfile> getSeekerProfile(String mobileNumber) {
        return seekerRepository.findByMobileNumber(mobileNumber);
    }

    public void updateLocation(String mobileNumber, double longitude, double latitude) {
        getWorkerProfile(mobileNumber).ifPresent(p -> {
            p.setLastLocation(new double[]{ longitude, latitude });
            workerRepository.save(p);
        });
    }

    public void updateAvailability(String mobileNumber, boolean available) {
        getWorkerProfile(mobileNumber).ifPresent(p -> {
            p.setAvailable(available);
            workerRepository.save(p);
            if (available) {
                // Publish event so Notification Service can notify worker about open jobs
                kafkaTemplate.send("worker.available", Map.of("mobileNumber", mobileNumber));
                log.debug("ProfileService: Published worker.available event for {}", mobileNumber);
            }
        });
    }

    public void updateDeviceToken(String mobileNumber, String token) {
        Optional<WorkerProfile> worker = getWorkerProfile(mobileNumber);
        if (worker.isPresent()) {
            worker.get().setDeviceToken(token);
            workerRepository.save(worker.get());
            return;
        }
        seekerRepository.findByMobileNumber(mobileNumber).ifPresent(p -> {
            p.setDeviceToken(token);
            seekerRepository.save(p);
        });
    }
}
