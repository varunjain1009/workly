package com.workly.modules.notification.repository;

import com.workly.modules.notification.model.UserToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserTokenRepository extends MongoRepository<UserToken, String> {
    Optional<UserToken> findByMobileNumber(String mobileNumber);
}
