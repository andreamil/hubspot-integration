package com.meetime.hubspot.controller;

import com.meetime.hubspot.config.HubSpotConfig;
import com.meetime.hubspot.service.HubSpotAuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // Importar
import org.springframework.web.servlet.view.RedirectView;

import java.util.UUID;

@Controller
@Slf4j
@RequiredArgsConstructor
public class HubSpotAuthController {

    private final HubSpotAuthService hubSpotAuthService;
    private final HubSpotConfig hubSpotConfig;
    private static final String OAUTH_STATE_ATTRIBUTE = "hubspot_oauth_state";

    @GetMapping("/authorize")
    public RedirectView initiateAuthorization(HttpSession session) {
        var clientId = (String) session.getAttribute(TestPageController.OVERRIDE_CLIENT_ID_ATTR);
        var useDefaultClientId = (clientId == null);
        if (useDefaultClientId) {
            clientId = hubSpotConfig.clientId();
        }

        if (clientId == null || clientId.isBlank()) {
            log.error("Client ID não configurado (nem default nem override)!");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "HubSpot Client ID não configurado.");
        }

        var state = UUID.randomUUID().toString();
        session.setAttribute(OAUTH_STATE_ATTRIBUTE, state);

        log.info("Iniciando fluxo OAuth 2.0 com Client ID {} e state: {}",
                useDefaultClientId ? "(default)" : "(override)", state);

        var authorizationUrl = hubSpotAuthService.generateAuthorizationUrl(state, clientId);
        return new RedirectView(authorizationUrl);
    }

    @SuppressWarnings("deprecation")
    @GetMapping("/oauth-callback")
    public RedirectView handleCallback(@RequestParam String code, @RequestParam String state,
                                       HttpSession session, RedirectAttributes redirectAttributes) {
        log.info("Recebido callback do HubSpot com code [{}] e state [{}].", code.length(), state.length());
        var storedState = (String) session.getAttribute(OAUTH_STATE_ATTRIBUTE);

        if (storedState == null || !storedState.equals(state)) {
            log.error("State inválido recebido no callback. State esperado: {}, State recebido: {}", storedState, state);
            clearSessionOverrides(session);
            redirectAttributes.addFlashAttribute("oauthError", true);
            redirectAttributes.addFlashAttribute("oauthErrorMessage", "State mismatch (possível CSRF).");
            return new RedirectView("/test-page");
        }

        log.debug("State validado com sucesso.");

        var effectiveClientId = (String) session.getAttribute(TestPageController.OVERRIDE_CLIENT_ID_ATTR);
        var effectiveClientSecret = (String) session.getAttribute(TestPageController.OVERRIDE_CLIENT_SECRET_ATTR);
        var usedOverrides = (effectiveClientId != null || effectiveClientSecret != null);

        if (effectiveClientId == null) effectiveClientId = hubSpotConfig.clientId();
        if (effectiveClientSecret == null) effectiveClientSecret = hubSpotConfig.clientSecret();

        clearSessionOverrides(session);

        if (effectiveClientId == null || effectiveClientId.isBlank() || effectiveClientSecret == null || effectiveClientSecret.isBlank()) {
            log.error("Client ID ou Secret não disponíveis (default ou override) para troca de token.");
            redirectAttributes.addFlashAttribute("oauthError", true);
            redirectAttributes.addFlashAttribute("oauthErrorMessage", "Credenciais HubSpot (Client ID/Secret) não configuradas no backend.");
            return new RedirectView("/test-page");
        }

        log.info("Tentando trocar código por token usando Client ID {}.", usedOverrides ? "(override)" : "(default)");

        try {
            hubSpotAuthService.exchangeCodeForTokens(code, effectiveClientId, effectiveClientSecret).block();
            log.info("Troca de código por token bem-sucedida.");
            redirectAttributes.addFlashAttribute("oauthSuccess", true);
            return new RedirectView("/test-page");
        } catch (Exception e) {
            log.error("Falha ao processar o callback do OAuth: {}", e.getMessage(), e);
            String errorMessage = extractErrorMessage(e);
            redirectAttributes.addFlashAttribute("oauthError", true);
            redirectAttributes.addFlashAttribute("oauthErrorMessage", "Falha na troca de token: " + errorMessage);
            return new RedirectView("/test-page");
        }
    }

    private String extractErrorMessage(Throwable e) {
        if (e instanceof WebClientResponseException wcre) {
            return String.format("Erro HubSpot [%d] - %s", wcre.getStatusCode().value(), wcre.getResponseBodyAsString());
        } else if (e.getMessage() != null) {
            return e.getMessage();
        }
        return e.getClass().getSimpleName();
    }


    private void clearSessionOverrides(HttpSession session) {
        session.removeAttribute(OAUTH_STATE_ATTRIBUTE);
        session.removeAttribute(TestPageController.OVERRIDE_CLIENT_ID_ATTR);
        session.removeAttribute(TestPageController.OVERRIDE_CLIENT_SECRET_ATTR);
        log.debug("Atributos de override/state da sessão limpos.");
    }
}