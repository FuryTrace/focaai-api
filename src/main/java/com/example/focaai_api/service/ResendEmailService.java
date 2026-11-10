package com.example.focaai_api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Service
public class ResendEmailService {

    private final RestClient restClient;
    private final String remetente;

    public ResendEmailService(
            @Value("${resend.api-key}") String apiKey,
            @Value("${resend.from}") String remetente) {

        this.remetente = remetente;

        this.restClient = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public void enviarCodigo(String destinatario, String codigo) {

        String html = """
                <div style="font-family: Arial, sans-serif;">
                    <h2>Recuperação de senha - FocaAi</h2>
                    <p>Recebemos uma solicitação para recuperar sua senha.</p>
                    <p>Seu código de recuperação é:</p>
                    <h1 style="letter-spacing: 5px;">%s</h1>
                    <p>Este código expira em 10 minutos.</p>
                    <p>Se você não solicitou esta recuperação,
                    ignore este e-mail.</p>
                </div>
                """.formatted(codigo);

        Map<String, Object> corpo = Map.of(
                "from", remetente,
                "to", List.of(destinatario),
                "subject", "Código de recuperação - FocaAi",
                "html", html
        );

        try {
            restClient.post()
                    .uri("/emails")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientException e) {
            throw new IllegalStateException(
                    "Não foi possível enviar o e-mail de recuperação.",
                    e
            );
        }
    }
}