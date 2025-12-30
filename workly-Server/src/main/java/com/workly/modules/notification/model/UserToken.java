package com.workly.modules.notification.model;

import com.workly.core.MongoBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "user_tokens")
public class UserToken extends MongoBaseEntity {
    @org.springframework.data.annotation.Id
    private String id;

    private String mobileNumber; // Identifying the user
    private String fcmToken;

    private String configVersion;
    private Long lastConfigNotificationTime;
    private Long lastSyncedTime;
}
