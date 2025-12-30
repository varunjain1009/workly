package com.workly.modules.notification.service;

import com.workly.modules.notification.model.UserToken;
import com.workly.modules.notification.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserTokenService {
    private final UserTokenRepository userTokenRepository;

    public void saveToken(String mobileNumber, String fcmToken) {
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
