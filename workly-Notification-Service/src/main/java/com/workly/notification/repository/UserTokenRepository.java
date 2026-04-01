package com.workly.notification.repository;

import com.workly.notification.model.UserToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserTokenRepository extends MongoRepository<UserToken, String> {
    Optional<UserToken> findByMobileNumber(String mobileNumber);
}
