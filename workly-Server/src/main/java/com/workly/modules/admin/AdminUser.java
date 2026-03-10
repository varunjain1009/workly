package com.workly.modules.admin;

import com.workly.core.MongoBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "admin_users")
public class AdminUser extends MongoBaseEntity {
    @Id
    private String id;
    
    private String username;
    private String passwordHash;
}
