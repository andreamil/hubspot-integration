package com.meetime.hubspot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class HubSpotTokenResponseDTO {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("expires_in")
    private long expiresIn;

    private transient Instant expiresAt;

    // Método para calcular o momento de expiração
    public void calculateExpiresAt() {
        if (expiresIn > 0) {
            // Adiciona uma pequena margem para evitar usar o token exatamente no limite
            this.expiresAt = Instant.now().plusSeconds(expiresIn - 60);
        }
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}