package com.workly.modules.promotion;

import com.workly.core.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
@Slf4j
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping("/validate")
    public ApiResponse<PromotionValidationResult> validatePromotion(
            @RequestParam String code,
            @RequestParam double amount) {

        log.debug("PromotionController: [ENTER] validatePromotion - code: {}, amount: {}", code, amount);
        PromotionValidationResult result = promotionService.validatePromotion(code, amount);
        log.debug("PromotionController: [EXIT] validatePromotion - discount: {}", result.getDiscountAmount());
        
        return ApiResponse.success(result, "Promotion validated applied successfully");
    }
}
