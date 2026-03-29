package com.workly.modules.monetization;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonetizationService {

    private final SubscriptionRepository subscriptionRepository;

    @Value("${monetisation.enabled}")
    private boolean monetizationEnabled;

    public boolean isUserAuthorized(String mobileNumber) {
        log.debug("MonetizationService: [ENTER] isUserAuthorized - mobile: {}, featureEnabled: {}", mobileNumber, monetizationEnabled);
        if (!monetizationEnabled) {
            log.debug("MonetizationService: [EXIT] Monetization disabled globally, granting access");
            return true;
        }

        boolean authorized = subscriptionRepository.findTopByUserMobileOrderByExpiryDateDesc(mobileNumber)
                .map(sub -> sub.isActive() && sub.getExpiryDate().isAfter(LocalDateTime.now()))
                .orElse(false);
        log.debug("MonetizationService: [EXIT] isUserAuthorized - result: {}", authorized);
        return authorized;
    }

    public Subscription subscribe(String mobileNumber, String planType, int durationDays) {
        log.debug("MonetizationService: [ENTER] subscribe - mobile: {}, plan: {}, duration: {} days", mobileNumber, planType, durationDays);
        Subscription sub = new Subscription();
        sub.setUserMobile(mobileNumber);
        sub.setPlanType(planType);
        sub.setActive(true);
        sub.setExpiryDate(LocalDateTime.now().plusDays(durationDays));
        Subscription saved = subscriptionRepository.save(sub);
        log.debug("MonetizationService: [EXIT] subscribe - Subscription activated until {}", saved.getExpiryDate());
        return saved;
    }
}
