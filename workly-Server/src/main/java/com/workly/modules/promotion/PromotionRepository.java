package com.workly.modules.promotion;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface PromotionRepository extends MongoRepository<Promotion, String> {
    Optional<Promotion> findByCodeAndActiveTrue(String code);
}
