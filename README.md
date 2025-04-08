# Integração HubSpot - Case Técnico Meetime (Java 17 / Spring Boot 3)

Esta é uma aplicação Spring Boot que demonstra a integração com a API do HubSpot, conforme especificado no case técnico da Meetime, utilizando Java 17 e Spring Boot 3.4.

## Funcionalidades Implementadas

1.  **Fluxo de Autenticação OAuth 2.0 (Authorization Code Flow):**
    *   `GET /authorize`: Inicia o fluxo, redirecionando o usuário para a página de autorização do HubSpot. Leva em conta overrides de credenciais definidos na sessão via página de teste.
    *   `GET /oauth-callback`: Recebe o callback do HubSpot após a autorização, valida o parâmetro `state`, troca o código de autorização por tokens (access e refresh) usando as credenciais corretas (override ou default) e armazena os tokens (em memória neste exemplo). Redireciona para a página de teste com status.
2.  **Criação de Contatos:**
    *   `POST /contacts`: Endpoint para criar um novo contato no HubSpot. Requer um corpo JSON com as propriedades do contato (ex: `email`, `firstname`, `lastname`). Utiliza o access token obtido via OAuth e implementa refresh automático de token se necessário. Inclui tratamento básico de rate limit (retry).
3.  **Recebimento de Webhooks:**
    *   `POST /webhooks/hubspot-events`: Endpoint para receber notificações de webhook do HubSpot (ex: `contact.creation`). Valida a assinatura do webhook (`X-HubSpot-Signature` ou `X-HubSpot-Signature-v3`) para segurança e processa os eventos recebidos (simplesmente loga neste exemplo).
4.  **Página de Teste:**
    *   `GET /test-page`: Serve uma página HTML simples para facilitar o teste interativo de todo o fluxo. Permite iniciar o OAuth com credenciais padrão ou overrides temporários (armazenados na sessão) e criar contatos via formulário. Exibe mensagens de status e erro.
    *   `POST /set-test-credentials`: Endpoint auxiliar usado pela página de teste para armazenar credenciais de override na sessão antes de iniciar o fluxo OAuth.

## Pré-requisitos

*   Java 17 (ou superior)
*   Maven 3.8 (ou superior)
*   Conta de Desenvolvedor HubSpot ([HubSpot Developer Account](https://developers.hubspot.com/signup))
*   Um Aplicativo (App) criado na sua conta de desenvolvedor HubSpot, com:
    *   Client ID e Client Secret anotados.
    *   URI de Redirecionamento (`Redirect URI`) configurada para apontar para `/oauth-callback` da sua aplicação (ex: `http://localhost:8080/oauth-callback` para desenvolvimento local).
    *   Scopes definidos (ex: `crm.objects.contacts.write`, `crm.objects.contacts.read`, `oauth`).
    *   Uma assinatura de Webhook configurada para o evento desejado (`contact.creation`) apontando para o endpoint `/webhooks/hubspot-events` da sua aplicação (requer URL pública - use `ngrok` para teste local). Configure a versão da assinatura (v1, v2 ou v3) no HubSpot e no `application.properties`.

## Configuração

1.  **Clonar o Repositório:**
    ```bash
    git clone <url-do-repositorio>
    cd hubspot-integration
    ```
2.  **Configurar Credenciais HubSpot (Padrão):**
    *   Edite o arquivo `src/main/resources/application.properties`.
    *   Substitua os valores de `hubspot.client-id` e `hubspot.client-secret` pelas credenciais *padrão* do seu aplicativo HubSpot.
    *   Verifique se `hubspot.redirect-uri` corresponde exatamente à URI configurada no seu App HubSpot.
    *   Ajuste `hubspot.scopes` se necessário.
    *   Ajuste `hubspot.webhook.signature-version` (1, 2 ou 3) para corresponder à versão configurada no HubSpot.

## Como Construir e Executar

1.  **Construir o Projeto (usando Maven):**
    ```bash
    mvn clean package
    ```
2.  **Executar a Aplicação:**
    ```bash
    java -jar target/hubspot-integration-*.jar
    ```
    A aplicação estará disponível em `http://localhost:8080`.

##  Página de Teste (`/test-page`)

1.  Abra seu navegador e acesse `http://localhost:8080/test-page`.
2.  **Autorização OAuth:**
    *   **Padrão:** Clique em "Iniciar Autorização (Padrão)".
    *   **Com Overrides:** Preencha os campos "Override Client ID" e/ou "Override Client Secret" e clique em "Iniciar Autorização com Overrides". As credenciais de override são temporárias e armazenadas apenas na sessão do navegador.
    *   Siga o fluxo de autorização no HubSpot. Você será redirecionado de volta para a página de teste com uma mensagem de sucesso ou erro.
3.  **Criar Contato:**
    *   Após autorizar com sucesso, preencha o formulário na seção "Criar Contato".
    *   Clique em "Criar Contato".
    *   Uma mensagem de status (sucesso ou erro, incluindo detalhes da API HubSpot) será exibida abaixo dos formulários.
4.  **Testar Webhook:**
    *   Configure o webhook no HubSpot App apontando para a URL pública da sua aplicação (`ngrok` + `/webhooks/hubspot-events`).
    *   Crie um contato (pelo formulário da página de teste ou pela UI do HubSpot).
    *   Verifique os **logs da aplicação Spring Boot** no console para ver as mensagens indicando o recebimento e processamento do evento de webhook (incluindo a validação da assinatura).


## Considerações

Este exemplo utiliza um `Map` em memória (`ConcurrentHashMap`) para armazenar os tokens OAuth. **Isso NÃO é adequado para produção** pois os tokens são perdidos ao reiniciar, não escala e não suporta múltiplos usuários.

**Melhorias para Produção:**
*   Armazenar tokens em um banco de dados seguro.
*   Associar tokens a um identificador de usuário/conta.
*   Criptografar `refresh_token` antes de persistir.

*   **Endpoint `/contacts`:** Para simplificar a página de teste, o endpoint `POST /contacts` está configurado para permitir acesso sem autenticação no nível do Spring Security (`permitAll()`). A segurança real da operação depende da validação do token de acesso HubSpot dentro do serviço.
