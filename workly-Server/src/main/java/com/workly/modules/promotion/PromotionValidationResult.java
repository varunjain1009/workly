package com.workly.modules.promotion;

import lombok.Data;

@Data
public class PromotionValidationResult {
    private boolean valid;
    private String code;
    private double originalAmount;
    private double discountAmount;
    private double finalAmount;
}
