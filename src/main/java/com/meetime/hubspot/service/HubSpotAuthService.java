package com.meetime.hubspot.service;

import com.meetime.hubspot.config.HubSpotConfig;
import com.meetime.hubspot.dto.HubSpotTokenResponseDTO;
// Removido import de HubspotApiException
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;


@Service
@Slf4j
@RequiredArgsConstructor
public class HubSpotAuthService {

    private final HubSpotConfig hubSpotConfig;
    private final WebClient webClient;
    private final HubSpotTokenService tokenService;

    public String generateAuthorizationUrl(String state, String clientId) {
        var url = UriComponentsBuilder
                .fromHttpUrl(hubSpotConfig.oauthAuthorizeUrl())
                .queryParam("client_id", clientId)
                .queryParam("scope", hubSpotConfig.scopes())
                .queryParam("redirect_uri", hubSpotConfig.redirectUri())
                .queryParam("state", state)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
        log.debug("Generated HubSpot Authorization URL: {}", url);
        return url;
    }

    public Mono<HubSpotTokenResponseDTO> exchangeCodeForTokens(String code, String clientId, String clientSecret) {
        var formData = new LinkedMultiValueMap<String, String>();
        formData.add("grant_type", "authorization_code");
        formData.add("code", code);
        formData.add("redirect_uri", hubSpotConfig.redirectUri());
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);

        log.info("Trocando código de autorização por tokens...");
        return webClient.post()
                .uri(hubSpotConfig.oauthTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        // Apenas re-lança a exceção original
                        response -> response.createException().flatMap(Mono::error)
                )
                .bodyToMono(HubSpotTokenResponseDTO.class)
                .doOnNext(tokenDto -> {
                    log.info("Tokens recebidos com sucesso.");
                    tokenService.storeTokens(tokenDto);
                })
                .doOnError(e -> {
                    // Log apenas se não for WebClientResponseException (já logado no handler)
                    if (!(e instanceof WebClientResponseException)) {
                        log.error("Erro inesperado durante a troca de código por token.", e);
                    }
                });
    }
}