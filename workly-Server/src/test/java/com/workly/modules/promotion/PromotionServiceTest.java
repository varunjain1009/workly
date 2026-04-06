package com.workly.modules.promotion;

import com.workly.core.WorklyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PromotionServiceTest {

    @Mock private PromotionRepository promotionRepository;

    private PromotionService promotionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        promotionService = new PromotionService(promotionRepository);
    }

    private Promotion activePromotion(double pct, double maxDiscount, LocalDateTime expiry) {
        Promotion p = new Promotion();
        p.setCode("SAVE20");
        p.setDiscountPercentage(pct);
        p.setMaxDiscountAmount(maxDiscount);
        p.setExpirationDate(expiry);
        p.setActive(true);
        return p;
    }

    @Test
    void validatePromotion_success_noMaxCap() {
        Promotion p = activePromotion(20.0, 0, null);
        when(promotionRepository.findByCodeAndActiveTrue("SAVE20")).thenReturn(Optional.of(p));

        PromotionValidationResult result = promotionService.validatePromotion("SAVE20", 100.0);

        assertTrue(result.isValid());
        assertEquals(20.0, result.getDiscountAmount());
        assertEquals(80.0, result.getFinalAmount());
    }

    @Test
    void validatePromotion_success_withMaxCap_discountCapped() {
        Promotion p = activePromotion(50.0, 30.0, null);
        when(promotionRepository.findByCodeAndActiveTrue("SAVE20")).thenReturn(Optional.of(p));

        PromotionValidationResult result = promotionService.validatePromotion("SAVE20", 100.0);

        assertEquals(30.0, result.getDiscountAmount()); // capped at 30
        assertEquals(70.0, result.getFinalAmount());
    }

    @Test
    void validatePromotion_success_withMaxCap_notExceeded() {
        Promotion p = activePromotion(10.0, 50.0, null);
        when(promotionRepository.findByCodeAndActiveTrue("SAVE20")).thenReturn(Optional.of(p));

        PromotionValidationResult result = promotionService.validatePromotion("SAVE20", 100.0);

        assertEquals(10.0, result.getDiscountAmount()); // 10% of 100 = 10, under cap
        assertEquals(90.0, result.getFinalAmount());
    }

    @Test
    void validatePromotion_notFound_throws() {
        when(promotionRepository.findByCodeAndActiveTrue("INVALID")).thenReturn(Optional.empty());
        assertThrows(WorklyException.class, () -> promotionService.validatePromotion("INVALID", 100.0));
    }

    @Test
    void validatePromotion_expired_throws() {
        Promotion p = activePromotion(20.0, 0, LocalDateTime.now().minusDays(1));
        when(promotionRepository.findByCodeAndActiveTrue("SAVE20")).thenReturn(Optional.of(p));

        assertThrows(WorklyException.class, () -> promotionService.validatePromotion("SAVE20", 100.0));
    }

    @Test
    void validatePromotion_notExpired_success() {
        Promotion p = activePromotion(10.0, 0, LocalDateTime.now().plusDays(1));
        when(promotionRepository.findByCodeAndActiveTrue("SAVE20")).thenReturn(Optional.of(p));

        PromotionValidationResult result = promotionService.validatePromotion("SAVE20", 100.0);
        assertTrue(result.isValid());
    }
}
