package com.workly.modules.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/config/features")
public class FeatureConfigController {

    @Value("${workly.features.payments.enabled:false}")
    private boolean paymentsEnabled;

    @Value("${workly.features.sms.enabled:false}")
    private boolean smsEnabled;

    @GetMapping
    public Map<String, Boolean> getFeatureFlags() {
        Map<String, Boolean> flags = new HashMap<>();
        flags.put("paymentsEnabled", paymentsEnabled);
        flags.put("smsEnabled", smsEnabled);
        return flags;
    }
}
