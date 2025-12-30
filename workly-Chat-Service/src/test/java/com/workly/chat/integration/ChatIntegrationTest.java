package com.workly.chat.integration;

import com.workly.chat.model.Message;
import com.workly.chat.repository.MessageRepository;
import com.workly.chat.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class ChatIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:6.0");

    @Container
    @ServiceConnection
    @SuppressWarnings("deprecation")
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @Autowired
    private ChatService chatService;

    @Autowired
    private MessageRepository messageRepository;

    @Test
    void shouldPersistAndPublishMessage() {
        Message message = new Message();
        message.setSenderId("user1");
        message.setReceiverId("user2");
        message.setContent("Hello Integration");

        chatService.saveMessage(message);

        // Verify Persistence
        assertThat(messageRepository.findAll()).hasSize(1);

        // Verification of Kafka publishing would typically involve a test Consumer
        // For now, ensuring no exception occurred and data is in DB is a good start.
        // To verify Kafka, we'd add a @KafkaListener in test config.
    }
}
