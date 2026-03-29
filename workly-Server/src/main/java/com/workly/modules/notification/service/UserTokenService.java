package com.workly.modules.notification.service;

import com.workly.modules.notification.model.UserToken;
import com.workly.modules.notification.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserTokenService {
    private final UserTokenRepository userTokenRepository;

    public void saveToken(String mobileNumber, String fcmToken) {
        log.debug("UserTokenService: [ENTER] saveToken - mobile: {}, token prefix: {}", mobileNumber, fcmToken != null ? fcmToken.substring(0, Math.min(10, fcmToken.length())) + "..." : "null");
        Optional<UserToken> existing = userTokenRepository.findByMobileNumber(mobileNumber);
        UserToken token = existing.orElse(new UserToken());
        token.setMobileNumber(mobileNumber);
        token.setFcmToken(fcmToken);
        userTokenRepository.save(token);
        log.debug("UserTokenService: [EXIT] saveToken - Token persisted for {}", mobileNumber);
    }

    public String getToken(String mobileNumber) {
        log.debug("UserTokenService: [ENTER] getToken - Querying FCM token for mobile: {}", mobileNumber);
        String result = userTokenRepository.findByMobileNumber(mobileNumber)
                .map(UserToken::getFcmToken)
                .orElse(null);
        log.debug("UserTokenService: [EXIT] getToken - Found: {}", result != null);
        return result;
    }
}
