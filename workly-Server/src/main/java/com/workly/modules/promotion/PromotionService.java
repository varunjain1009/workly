package com.workly.modules.promotion;

import com.workly.core.WorklyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;

    public PromotionValidationResult validatePromotion(String code, double jobBudget) {
        log.debug("PromotionService: Validating promotion code: {} for budget: {}", code, jobBudget);

        Promotion promotion = promotionRepository.findByCodeAndActiveTrue(code)
                .orElseThrow(() -> WorklyException.notFound("Promotion code not found or inactive"));

        if (promotion.getExpirationDate() != null && promotion.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw WorklyException.badRequest("Promotion code has expired");
        }

        double calculatedDiscount = jobBudget * (promotion.getDiscountPercentage() / 100.0);
        
        // Cap the discount if a max amount is set
        if (promotion.getMaxDiscountAmount() > 0 && calculatedDiscount > promotion.getMaxDiscountAmount()) {
            calculatedDiscount = promotion.getMaxDiscountAmount();
        }

        double finalAmount = Math.max(0, jobBudget - calculatedDiscount);

        PromotionValidationResult result = new PromotionValidationResult();
        result.setValid(true);
        result.setCode(code);
        result.setOriginalAmount(jobBudget);
        result.setDiscountAmount(calculatedDiscount);
        result.setFinalAmount(finalAmount);

        log.info("PromotionService: Code {} validated. Discount: {}", code, calculatedDiscount);
        return result;
    }
}
