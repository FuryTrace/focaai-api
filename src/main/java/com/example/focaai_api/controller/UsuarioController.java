package com.example.focaai_api.controller;

import com.example.focaai_api.dto.CadastroUsuarioRequest;
import com.example.focaai_api.dto.ExcluirContaRequest;
import com.example.focaai_api.dto.LoginRequest;
import com.example.focaai_api.dto.UsuarioResponse;
import com.example.focaai_api.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/cadastro")
    public ResponseEntity<UsuarioResponse> cadastrar(
            @Valid @RequestBody CadastroUsuarioRequest request) {

        UsuarioResponse resposta = usuarioService.cadastrar(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resposta);
    }

    @PostMapping("/login")
    public ResponseEntity<UsuarioResponse> login(
            @Valid @RequestBody LoginRequest request) {

        UsuarioResponse resposta = usuarioService.login(request);

        return ResponseEntity.ok(resposta);
    }

    @PostMapping("/excluir")
    public ResponseEntity<Void> excluirConta(
            @Valid @RequestBody ExcluirContaRequest request) {

        usuarioService.excluirConta(request);

        return ResponseEntity.noContent().build();
    }
}