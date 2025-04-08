package com.meetime.hubspot.util;

import com.meetime.hubspot.config.HubSpotConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat; // Java 17+ HexFormat

@Component
@Slf4j
@RequiredArgsConstructor
public class HubSpotSignatureVerifier {

    private final HubSpotConfig hubSpotConfig;
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final HexFormat hexFormat = HexFormat.of().withLowerCase();

    public boolean isValidSignature(String signatureHeader, String requestBody, String requestUri, String httpMethod) {
        if (signatureHeader == null || requestBody == null) {
            log.warn("Assinatura ou corpo da requisição ausentes para validação do webhook.");
            return false;
        }

        var clientSecret = hubSpotConfig.clientSecret();
        var signatureVersion = hubSpotConfig.webhookSignatureVersion();

        String sourceString;
        // TODO: Implementar lógica V3 completa se necessário (requer timestamp do header)
        if (signatureVersion == 3) {
            log.warn("Validação V3 de assinatura de webhook não implementada (requer timestamp). Usando lógica V1/V2 como fallback.");
            sourceString = clientSecret + requestBody;
        } else {
            sourceString = clientSecret + requestBody;
        }

        try {
            var mac = Mac.getInstance(HMAC_SHA256);
            var secretKeySpec = new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(sourceString.getBytes(StandardCharsets.UTF_8));
            var calculatedSignature = hexFormat.formatHex(hash);

            log.debug("Calculated Signature: {}", calculatedSignature);
            log.debug("Received Signature:   {}", signatureHeader);

            // Comparação segura
            return MessageDigest.isEqual(
                    calculatedSignature.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8)
            );

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Erro criptográfico ao calcular HMAC SHA256 para validação do webhook: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("Erro inesperado durante validação da assinatura: {}", e.getMessage(), e);
            return false;
        }
    }
}