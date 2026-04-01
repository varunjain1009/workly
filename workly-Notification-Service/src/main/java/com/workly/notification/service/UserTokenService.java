package com.workly.notification.service;

import com.workly.notification.model.UserToken;
import com.workly.notification.repository.UserTokenRepository;
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
        log.debug("UserTokenService: saving FCM token for {}", mobileNumber);
        Optional<UserToken> existing = userTokenRepository.findByMobileNumber(mobileNumber);
        UserToken token = existing.orElse(new UserToken());
        token.setMobileNumber(mobileNumber);
        token.setFcmToken(fcmToken);
        userTokenRepository.save(token);
    }

    public String getToken(String mobileNumber) {
        return userTokenRepository.findByMobileNumber(mobileNumber)
                .map(UserToken::getFcmToken)
                .orElse(null);
    }
}
