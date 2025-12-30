package com.workly.modules.monetization;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MonetizationService {

    private final SubscriptionRepository subscriptionRepository;

    @Value("${monetisation.enabled}")
    private boolean monetizationEnabled;

    public boolean isUserAuthorized(String mobileNumber) {
        if (!monetizationEnabled) {
            return true;
        }

        return subscriptionRepository.findTopByUserMobileOrderByExpiryDateDesc(mobileNumber)
                .map(sub -> sub.isActive() && sub.getExpiryDate().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    public Subscription subscribe(String mobileNumber, String planType, int durationDays) {
        Subscription sub = new Subscription();
        sub.setUserMobile(mobileNumber);
        sub.setPlanType(planType);
        sub.setActive(true);
        sub.setExpiryDate(LocalDateTime.now().plusDays(durationDays));
        return subscriptionRepository.save(sub);
    }
}
