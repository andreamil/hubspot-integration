package com.meetime.hubspot.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetime.hubspot.config.HubSpotConfig;
import com.meetime.hubspot.dto.WebhookEventDTO;
import com.meetime.hubspot.service.WebhookService;
import com.meetime.hubspot.util.HubSpotSignatureVerifier;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/webhooks")
@Slf4j
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;
    private final HubSpotSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper; // Injetado pelo Spring Boot
    private final HubSpotConfig hubSpotConfig;


    // Ajuste o path conforme configurado no HubSpot App
    @PostMapping("/hubspot-events")
    public ResponseEntity<String> handleHubSpotWebhook(
            @RequestBody String requestBody,
            @RequestHeader(value = "X-HubSpot-Signature-v3", required = false) String signatureV3,
            @RequestHeader(value = "X-HubSpot-Signature", required = false) String signatureV1V2,
            HttpServletRequest request) {

        String signature = hubSpotConfig.webhookSignatureVersion() == 3 ? signatureV3 : signatureV1V2;
        String requestUri = request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : "");

        log.info("Webhook recebido em {}", requestUri);
        log.debug("Webhook Body: {}", requestBody);
        log.debug("Webhook Signature Header (v{}): {}", hubSpotConfig.webhookSignatureVersion(), signature);


        // 1. Validar Assinatura
        if (!signatureVerifier.isValidSignature(signature, requestBody, requestUri, request.getMethod())) {
            log.warn("Assinatura do Webhook inválida. Abortando processamento.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Assinatura inválida.");
        }
        log.info("Assinatura do Webhook validada com sucesso.");

        // 2. Deserializar Payload
        try {
            List<WebhookEventDTO> events = objectMapper.readValue(requestBody, new TypeReference<List<WebhookEventDTO>>() {});

            // 3. Processar Eventos (Assíncrono seria ideal)
            webhookService.processHubSpotEvents(events);

            // 4. Retornar 200 OK para HubSpot
            return ResponseEntity.ok("Webhook recebido e processado.");

        } catch (IOException e) {
            log.error("Erro ao deserializar o corpo do webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erro ao processar payload do webhook.");
        } catch (Exception e) {
            log.error("Erro inesperado ao processar o webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno ao processar webhook.");
        }
    }
}