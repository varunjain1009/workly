package com.workly.modules.profile;

import com.workly.core.MongoBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "skill_seeker_profiles")
public class SkillSeekerProfile extends MongoBaseEntity {
    @Id
    private String id;
    private String mobileNumber;
    private String name;
    private String email;
    private String profilePictureUrl;
    private String deviceToken;
}
