package com.workly.config;

import com.workly.modules.tracking.TrackingRedisSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@RequiredArgsConstructor
public class RedisListenerConfig {

    private final ConfigUpdateListener configUpdateListener;
    private final TrackingRedisSubscriber trackingRedisSubscriber;

    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(configUpdateListener, new PatternTopic("config_updates"));
        // Fan out provider location updates to seeker sessions on any JVM instance
        container.addMessageListener(trackingRedisSubscriber, new PatternTopic("tracking:*"));
        return container;
    }
}
