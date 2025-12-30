package com.workly.modules.monetization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MonetizationServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    private MonetizationService monetizationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        monetizationService = new MonetizationService(subscriptionRepository);
        ReflectionTestUtils.setField(monetizationService, "monetizationEnabled", true);
    }

    @Test
    void isUserAuthorized_ShouldReturnTrueIfNoMonetization() {
        ReflectionTestUtils.setField(monetizationService, "monetizationEnabled", false);
        assertTrue(monetizationService.isUserAuthorized("123"));
    }

    @Test
    void isUserAuthorized_ShouldReturnTrueWithActiveSubscription() {
        Subscription sub = new Subscription();
        sub.setActive(true);
        sub.setExpiryDate(LocalDateTime.now().plusDays(1));

        when(subscriptionRepository.findTopByUserMobileOrderByExpiryDateDesc("123")).thenReturn(Optional.of(sub));

        assertTrue(monetizationService.isUserAuthorized("123"));
    }

    @Test
    void isUserAuthorized_ShouldReturnFalseWithExpiredSubscription() {
        Subscription sub = new Subscription();
        sub.setActive(true);
        sub.setExpiryDate(LocalDateTime.now().minusDays(1));

        when(subscriptionRepository.findTopByUserMobileOrderByExpiryDateDesc("123")).thenReturn(Optional.of(sub));

        assertFalse(monetizationService.isUserAuthorized("123"));
    }

    @Test
    void subscribe_ShouldCreateSubscription() {
        String mobile = "123";
        String planType = "PREMIUM";
        int duration = 30;

        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));

        Subscription result = monetizationService.subscribe(mobile, planType, duration);

        assertEquals(mobile, result.getUserMobile());
        assertEquals(planType, result.getPlanType());
        assertTrue(result.isActive());
        assertNotNull(result.getExpiryDate());
        verify(subscriptionRepository).save(any(Subscription.class));
    }
}
