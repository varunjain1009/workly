package com.workly.search.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;
import java.util.List;

@Data
@Document(indexName = "skills_index", createIndex = false)
@Setting(settingPath = "static/es-settings.json")
public class SkillDocument {
    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String canonicalName;

    @Field(type = FieldType.Text)
    private List<String> aliases; // Will rely on dynamic mapping or simple text for now

    @Field(type = FieldType.Keyword)
    private String phonetic;
}
