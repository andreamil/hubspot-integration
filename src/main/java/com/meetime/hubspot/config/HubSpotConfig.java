package com.meetime.hubspot.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "hubspot")
@Validated
public record HubSpotConfig(
        @NotBlank String clientId,
        @NotBlank String clientSecret,
        @NotBlank String redirectUri,
        @NotBlank String scopes,
        @NotBlank String apiBaseUrl,
        @NotBlank String oauthAuthorizeUrl,
        @NotBlank String oauthTokenUrl,
        @NotBlank String apiContactsUrl,
        @PositiveOrZero int webhookSignatureVersion
) {
}