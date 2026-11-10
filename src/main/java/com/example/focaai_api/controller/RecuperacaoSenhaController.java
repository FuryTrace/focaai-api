package com.example.focaai_api.controller;

import com.example.focaai_api.dto.RedefinirSenhaRequest;
import com.example.focaai_api.dto.SolicitarRecuperacaoRequest;
import com.example.focaai_api.dto.TokenRecuperacaoResponse;
import com.example.focaai_api.dto.ValidarCodigoRequest;
import com.example.focaai_api.service.RecuperacaoSenhaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/usuarios/recuperacao")
public class RecuperacaoSenhaController {

    private final RecuperacaoSenhaService recuperacaoService;

    public RecuperacaoSenhaController(
            RecuperacaoSenhaService recuperacaoService) {
        this.recuperacaoService = recuperacaoService;
    }

    @PostMapping("/solicitar")
    public ResponseEntity<Map<String, String>> solicitar(
            @Valid @RequestBody SolicitarRecuperacaoRequest request) {

        String mensagem = recuperacaoService.solicitar(request);

        return ResponseEntity.ok(Map.of("mensagem", mensagem));
    }

    @PostMapping("/validar")
    public ResponseEntity<TokenRecuperacaoResponse> validar(
            @Valid @RequestBody ValidarCodigoRequest request) {

        return ResponseEntity.ok(
                recuperacaoService.validar(request)
        );
    }

    @PostMapping("/redefinir")
    public ResponseEntity<Map<String, String>> redefinir(
            @Valid @RequestBody RedefinirSenhaRequest request) {

        recuperacaoService.redefinir(request);

        return ResponseEntity.ok(
                Map.of("mensagem", "Senha redefinida com sucesso.")
        );
    }
}