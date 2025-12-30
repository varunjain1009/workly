package com.workly.config.repository;

import com.workly.config.model.ConfigEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfigRepository extends MongoRepository<ConfigEntity, String> {
    Optional<ConfigEntity> findByKeyAndScopeAndActiveTrue(String key, String scope);

    List<ConfigEntity> findByKeyAndScopeOrderByVersionDesc(String key, String scope);

    Optional<ConfigEntity> findByKeyAndScopeAndVersion(String key, String scope, Integer version);
}
