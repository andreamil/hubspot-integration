package com.meetime.hubspot.service;

import com.meetime.hubspot.config.HubSpotConfig;
import com.meetime.hubspot.dto.HubSpotTokenResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class HubSpotTokenService {

    private final Map<String, HubSpotTokenResponseDTO> tokenStorage = new ConcurrentHashMap<>();
    private static final String DEFAULT_USER_ID = "default_user";

    private final WebClient webClient;
    private final HubSpotConfig hubSpotConfig;

    public void storeTokens(HubSpotTokenResponseDTO tokens) {
        tokens.calculateExpiresAt();
        tokenStorage.put(DEFAULT_USER_ID, tokens);
        log.info("Tokens armazenados/atualizados para {}", DEFAULT_USER_ID);
    }

    public Optional<String> getValidAccessToken() {
        HubSpotTokenResponseDTO currentTokens = tokenStorage.get(DEFAULT_USER_ID);

        if (currentTokens == null) {
            log.warn("Nenhum token encontrado para {}. Necessário autorizar.", DEFAULT_USER_ID);
            return Optional.empty();
        }

        if (currentTokens.isExpired()) {
            log.info("Access token expirado para {}. Tentando refresh...", DEFAULT_USER_ID);
            return refreshAccessToken().blockOptional();
        }

        log.debug("Retornando access token válido existente para {}", DEFAULT_USER_ID);
        return Optional.of(currentTokens.getAccessToken());
    }

    private Mono<String> refreshAccessToken() {
        HubSpotTokenResponseDTO currentTokens = tokenStorage.get(DEFAULT_USER_ID);
        if (currentTokens == null || currentTokens.getRefreshToken() == null) {
            log.error("Refresh token não encontrado para {}. Não é possível renovar.", DEFAULT_USER_ID);
            return Mono.error(new RuntimeException("Refresh token não disponível. Reautorize."));
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", hubSpotConfig.clientId());
        formData.add("client_secret", hubSpotConfig.clientSecret());
        formData.add("redirect_uri", hubSpotConfig.redirectUri());
        formData.add("refresh_token", currentTokens.getRefreshToken());

        log.info("Enviando requisição para refresh token para {}", DEFAULT_USER_ID);
        return webClient.post()
                .uri(hubSpotConfig.oauthTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Erro ao tentar refresh token ({}): {}", response.statusCode(), errorBody);
                                    tokenStorage.remove(DEFAULT_USER_ID); // Remove tokens inválidos
                                    return Mono.error(new RuntimeException("Falha ao renovar token: " + response.statusCode()));
                                })
                )
                .bodyToMono(HubSpotTokenResponseDTO.class)
                .doOnNext(this::storeTokens) // Armazena os novos tokens
                .map(HubSpotTokenResponseDTO::getAccessToken)
                .doOnError(e -> log.error("Erro durante o processo de refresh token.", e));
    }
}