package com.meetime.hubspot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookEventDTO {

    @JsonProperty("objectId")
    private Long objectId;

    @JsonProperty("subscriptionType")
    private String subscriptionType;

    @JsonProperty("portalId")
    private Long portalId;

    @JsonProperty("occurredAt")
    private Long occurredAt;

    @JsonProperty("propertyName")
    private String propertyName;

    @JsonProperty("propertyValue")
    private String propertyValue;


}