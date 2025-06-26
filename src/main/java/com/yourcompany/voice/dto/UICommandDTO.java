package com.yourcompany.voice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * UI command DTO
 */
public class UICommandDTO {
    
    @JsonProperty("action")
    private String action;
    
    @JsonProperty("target")
    private String target;
    
    @JsonProperty("parameters")
    private Map<String, Object> parameters;
    
    @JsonProperty("context")
    private String context;
    
    // Constructors
    public UICommandDTO() {}
    
    public UICommandDTO(String action, String target, Map<String, Object> parameters) {
        this.action = action;
        this.target = target;
        this.parameters = parameters;
    }
    
    // Getters and setters
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }
}