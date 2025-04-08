package com.meetime.hubspot.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // Trata erros de validação de @Valid nos DTOs de request
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "A entrada fornecida é inválida.");
        problemDetail.setTitle("Erro de Validação");
        problemDetail.setType(URI.create("/errors/validation-error"));
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = (error instanceof FieldError) ? ((FieldError) error).getField() : error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        problemDetail.setProperty("invalidFields", fieldErrors);
        log.warn("Erro de validação: {}", fieldErrors);
        return ResponseEntity.badRequest().body(problemDetail);
    }

    // Trata WebClientResponseException (erros da API HubSpot e outras chamadas WebClient)
    @ExceptionHandler(WebClientResponseException.class)
    public ProblemDetail handleWebClientResponseException(WebClientResponseException ex) {
        HttpStatusCode status = ex.getStatusCode();
        String externalResponseBody = ex.getResponseBodyAsString(StandardCharsets.UTF_8);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, "Erro ao comunicar com serviço externo.");
        problemDetail.setTitle("Erro de Serviço Externo (" + status.value() + ")");
        problemDetail.setType(URI.create("/errors/external-service-error"));
        problemDetail.setProperty("externalResponse", externalResponseBody); // Corpo da resposta externa
        problemDetail.setProperty("externalStatus", status.value());

        log.error("Erro na chamada de serviço externo [{}]: Status={}, Body='{}'",
                ex.getRequest() != null ? ex.getRequest().getURI() : "N/A",
                status,
                externalResponseBody);

        return problemDetail;
    }

    // Trata ResponseStatusException (lançados manualmente, como 401 por state inválido)
    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatusException(ResponseStatusException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason());
        problemDetail.setTitle("Erro da Aplicação"); // Título mais genérico
        log.warn("ResponseStatusException: Status={}, Reason={}", ex.getStatusCode(), ex.getReason());
        if (ex.getCause() != null) {
            problemDetail.setProperty("cause", ex.getCause().getMessage());
        }
        return problemDetail;
    }

    // Trata IllegalStateException (ex: token não encontrado antes de chamar API)
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalStateException(IllegalStateException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problemDetail.setTitle("Pré-condição Falhou");
        problemDetail.setType(URI.create("/errors/precondition-failed"));
        log.warn("IllegalStateException: {}", ex.getMessage());
        return problemDetail;
    }

    // Handler genérico para outras exceções não esperadas
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro inesperado no servidor.");
        problemDetail.setTitle("Erro Interno do Servidor");
        problemDetail.setType(URI.create("/errors/internal-server-error"));
        log.error("Erro inesperado não tratado: {}", ex.getMessage(), ex); // Log completo aqui
        return problemDetail;
    }

    // Garante que outros erros internos do Spring também retornem ProblemDetail
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {

        if (!(body instanceof ProblemDetail)) {
            ProblemDetail problemDetail = ProblemDetail.forStatus(statusCode);
            problemDetail.setTitle("Erro Interno do Framework");
            problemDetail.setDetail(ex.getMessage());
            body = problemDetail;
        }
        log.error("Erro interno do Spring: Status={}, Causa={}", statusCode, ex.getMessage());
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }
}