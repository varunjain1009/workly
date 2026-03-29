package com.workly.modules.payment;

import com.workly.core.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/intent/{jobId}")
    public ApiResponse<PaymentTransaction> createPaymentIntent(@PathVariable String jobId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();
        log.debug("PaymentController: [ENTER] createPaymentIntent - jobId: {}, mobile: {}", jobId, mobileNumber);

        PaymentTransaction tx = paymentService.createPaymentIntent(jobId, mobileNumber);

        log.debug("PaymentController: [EXIT] createPaymentIntent - txId: {}", tx.getId());
        return ApiResponse.success(tx, "Payment Intent created successfully");
    }

    @GetMapping("/provider/ledger")
    public ApiResponse<List<PaymentTransaction>> getProviderLedger() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = auth.getName();
        log.debug("PaymentController: [ENTER] getProviderLedger - mobile: {}", mobileNumber);

        List<PaymentTransaction> ledger = paymentService.getProviderLedger(mobileNumber);

        log.debug("PaymentController: [EXIT] getProviderLedger - found {} completed transactions", ledger.size());
        return ApiResponse.success(ledger, "Payout ledger retrieved");
    }
}
