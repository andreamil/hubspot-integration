package com.meetime.hubspot.controller;

import com.meetime.hubspot.config.HubSpotConfig;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // Importar Model
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // Para passar atributos no redirect
import org.springframework.web.servlet.view.RedirectView;

@Controller
@Slf4j
@RequiredArgsConstructor
public class TestPageController {

    public static final String OVERRIDE_CLIENT_ID_ATTR = "override_hubspot_client_id";
    public static final String OVERRIDE_CLIENT_SECRET_ATTR = "override_hubspot_client_secret";

    private final HubSpotConfig hubSpotConfig;

    @GetMapping("/test-page")
    public String getTestPage(Model model, @RequestParam(required = false) String oauthStatus) {
        model.addAttribute("defaultClientId", hubSpotConfig.clientId() != null ? hubSpotConfig.clientId() : "N/A");

        if ("success".equals(oauthStatus)) {
            model.addAttribute("oauthSuccess", true);
        }

        return "test-page";
    }

    @PostMapping("/set-test-credentials")
    public RedirectView setTestCredentialsAndAuthorize(
            @RequestParam(required = false) String overrideClientId,
            @RequestParam(required = false) String overrideClientSecret,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        log.info("Recebida solicitação para definir credenciais de teste na sessão.");

        session.removeAttribute(OVERRIDE_CLIENT_ID_ATTR);
        session.removeAttribute(OVERRIDE_CLIENT_SECRET_ATTR);

        boolean hasOverrides = false;
        if (StringUtils.hasText(overrideClientId)) {
            session.setAttribute(OVERRIDE_CLIENT_ID_ATTR, overrideClientId);
            log.debug("Override Client ID definido na sessão.");
            hasOverrides = true;
        }
        if (StringUtils.hasText(overrideClientSecret)) {
            session.setAttribute(OVERRIDE_CLIENT_SECRET_ATTR, overrideClientSecret);
            log.debug("Override Client Secret definido na sessão (tamanho: {}).", overrideClientSecret.length());
            hasOverrides = true;
        }

        if (hasOverrides) {
            log.info("Credenciais de override definidas na sessão. Redirecionando para /authorize.");
        } else {
            log.info("Nenhum override fornecido. Redirecionando para /authorize (usará defaults).");
        }

        return new RedirectView("/authorize");
    }
}