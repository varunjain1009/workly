package com.workly.search.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Data
@Document(collection = "skills")
public class Skill {
    @Id
    private String id;
    private String canonicalName;
    private List<String> aliases;
    private String phonetic;
    private String status;
}
