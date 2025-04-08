package com.meetime.hubspot.controller;

import com.meetime.hubspot.dto.ContactPropertiesDTO;
import com.meetime.hubspot.service.HubSpotContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientResponseException;


@RestController
@RequestMapping("/contacts")
@Slf4j
@RequiredArgsConstructor
public class ContactController {

    private final HubSpotContactService contactService;

    @PostMapping
    public ResponseEntity<?> createContact(@Valid @RequestBody ContactPropertiesDTO contactDto) {
        log.info("Recebida requisição para criar contato: {}", contactDto.email());
        try {
            String hubspotResponse = contactService.createContact(contactDto).block();
            return ResponseEntity.status(HttpStatus.CREATED).body(hubspotResponse);
        } catch (IllegalStateException e) {
            log.warn("Erro de estado ao criar contato: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Erro: " + e.getMessage());
        } catch (WebClientResponseException e) {
            log.error("Erro da API HubSpot ao criar contato: Status={}, Body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            String errorMessage = String.format("Erro da API HubSpot (%s): %s", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(errorMessage);
        } catch (Exception e) {
            log.error("Erro inesperado ao criar contato: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno ao processar a requisição.");
        }
    }
}