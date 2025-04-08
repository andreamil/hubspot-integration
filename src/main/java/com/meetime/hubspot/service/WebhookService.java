package com.meetime.hubspot.service;

import com.meetime.hubspot.dto.WebhookEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class WebhookService {

    public void processHubSpotEvents(List<WebhookEventDTO> events) {
        if (events == null || events.isEmpty()) {
            log.warn("Recebida lista de eventos de webhook vazia ou nula.");
            return;
        }

        log.info("Processando {} evento(s) recebido(s) via webhook.", events.size());

        for (WebhookEventDTO event : events) {
            try {
                if ("contact.creation".equalsIgnoreCase(event.getSubscriptionType())) {
                    handleContactCreation(event);
                } else if ("contact.propertyChange".equalsIgnoreCase(event.getSubscriptionType())) {
                    handleContactPropertyChange(event);
                } else {
                    log.warn("Tipo de evento não tratado recebido: {}", event.getSubscriptionType());
                }
            } catch (Exception e) {
                log.error("Erro ao processar evento de webhook (ObjectId: {}): {}", event.getObjectId(), e.getMessage(), e);
            }
        }
        log.info("Processamento de eventos do webhook concluído.");
    }

    private void handleContactCreation(WebhookEventDTO event) {
        // Lógica para quando um contato é criado
        log.info("Evento de Criação de Contato Recebido - ID: {}, Portal: {}", event.getObjectId(), event.getPortalId());
    }

    private void handleContactPropertyChange(WebhookEventDTO event) {
        // Lógica para quando uma propriedade de contato muda
        log.info("Evento de Mudança de Propriedade Recebido - ID: {}, Propriedade: {}, Novo Valor: {}",
                event.getObjectId(), event.getPropertyName(), event.getPropertyValue());
    }
}