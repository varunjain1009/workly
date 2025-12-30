package com.workly.modules.profile;

import lombok.RequiredArgsConstructor;
import com.workly.modules.search.SearchServiceClient;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final WorkerProfileRepository workerRepository;
    private final SkillSeekerProfileRepository seekerRepository;
    private final SearchServiceClient searchServiceClient;

    public WorkerProfile createOrUpdateWorkerProfile(WorkerProfile profile) {
        if (profile.getSkills() != null && !profile.getSkills().isEmpty()) {
            profile.setSkills(searchServiceClient.normalizeSkills(profile.getSkills()));
        }
        return workerRepository.save(profile);
    }

    public SkillSeekerProfile createOrUpdateSeekerProfile(SkillSeekerProfile profile) {
        return seekerRepository.save(profile);
    }

    public Optional<WorkerProfile> getWorkerProfile(String mobileNumber) {
        return workerRepository.findByMobileNumber(mobileNumber);
    }

    public Optional<SkillSeekerProfile> getSeekerProfile(String mobileNumber) {
        return seekerRepository.findByMobileNumber(mobileNumber);
    }

    public void updateLocation(String mobileNumber, double longitude, double latitude) {
        workerRepository.findByMobileNumber(mobileNumber).ifPresent(p -> {
            p.setLastLocation(new double[] { longitude, latitude });
            workerRepository.save(p);
        });
    }

    public void updateAvailability(String mobileNumber, boolean available) {
        workerRepository.findByMobileNumber(mobileNumber).ifPresent(p -> {
            p.setAvailable(available);
            workerRepository.save(p);
        });
    }

    public void updateDeviceToken(String mobileNumber, String token) {
        // Try to find worker first
        Optional<WorkerProfile> worker = workerRepository.findByMobileNumber(mobileNumber);
        if (worker.isPresent()) {
            WorkerProfile p = worker.get();
            p.setDeviceToken(token);
            workerRepository.save(p);
            return;
        }

        // Else try seeker
        Optional<SkillSeekerProfile> seeker = seekerRepository.findByMobileNumber(mobileNumber);
        if (seeker.isPresent()) {
            SkillSeekerProfile p = seeker.get();
            p.setDeviceToken(token);
            seekerRepository.save(p);
        }
    }
}
