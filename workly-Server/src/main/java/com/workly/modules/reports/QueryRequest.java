package com.workly.modules.reports;

import lombok.Data;

@Data
public class QueryRequest {
    private String type; // SQL or MONGO
    private String query;
    private String collection; // For Mongo only
}
