package com.meetime.hubspot.service;

import com.meetime.hubspot.config.HubSpotConfig;
import com.meetime.hubspot.dto.ContactPropertiesDTO;
import com.meetime.hubspot.dto.HubSpotContactRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class HubSpotContactService {

    private final WebClient webClient;
    private final HubSpotTokenService tokenService;
    private final HubSpotConfig hubSpotConfig;

    public Mono<String> createContact(ContactPropertiesDTO contactDto) {
        log.info("Tentando criar contato: {}", contactDto.email());

        return Mono.justOrEmpty(tokenService.getValidAccessToken())
                .switchIfEmpty(Mono.error(new IllegalStateException("Access Token inválido ou não disponível. Autorize primeiro.")))
                .flatMap(accessToken -> performContactCreation(accessToken, contactDto))
                .doOnError(e -> {
                    // Log apenas se não for WebClientResponseException (já logado no handler) ou IllegalStateException
                    if (!(e instanceof WebClientResponseException || e instanceof IllegalStateException)) {
                        log.error("Falha final inesperada ao criar contato no HubSpot para {}: {}", contactDto.email(), e.getMessage(), e);
                    }
                });
    }

    private Mono<String> performContactCreation(String accessToken, ContactPropertiesDTO contactDto) {
        var requestBody = new HubSpotContactRequestDTO(contactDto);

        return webClient.post()
                .uri(hubSpotConfig.apiContactsUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        response -> response.createException().flatMap(Mono::error)
                )
                .bodyToMono(String.class)
                .doOnSuccess(responseBody -> log.info("Contato criado com sucesso no HubSpot."))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(this::isRateLimitException)
                        .doBeforeRetry(retrySignal -> log.warn("Rate limit atingido (tentativa {}). Retentando...", retrySignal.totalRetries() + 1))
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            log.error("Retry esgotado para rate limit após {} tentativas.", retrySignal.totalRetries());
                            return new RuntimeException("Rate limit da API HubSpot excedido após múltiplas tentativas.", retrySignal.failure());
                        })
                );
    }

    private boolean isRateLimitException(Throwable throwable) {
        return throwable instanceof WebClientResponseException ex && ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS;
    }
}