package com.ima.web;

import com.fasterxml.jackson.databind.JsonNode;

public class SavedReportInput {
    private String name;
    private String type;
    private JsonNode definition;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public JsonNode getDefinition() {
        return definition;
    }

    public void setDefinition(JsonNode definition) {
        this.definition = definition;
    }
}
