package com.example.focaai_api.service;

import com.example.focaai_api.dto.RedefinirSenhaRequest;
import com.example.focaai_api.dto.SolicitarRecuperacaoRequest;
import com.example.focaai_api.dto.TokenRecuperacaoResponse;
import com.example.focaai_api.dto.ValidarCodigoRequest;
import com.example.focaai_api.model.RecuperacaoSenha;
import com.example.focaai_api.model.Usuario;
import com.example.focaai_api.repository.RecuperacaoSenhaRepository;
import com.example.focaai_api.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;

@Service
public class RecuperacaoSenhaService {

    private final UsuarioRepository usuarioRepository;
    private final RecuperacaoSenhaRepository recuperacaoRepository;
    private final ResendEmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public RecuperacaoSenhaService(
            UsuarioRepository usuarioRepository,
            RecuperacaoSenhaRepository recuperacaoRepository,
            ResendEmailService emailService) {

        this.usuarioRepository = usuarioRepository;
        this.recuperacaoRepository = recuperacaoRepository;
        this.emailService = emailService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public String solicitar(SolicitarRecuperacaoRequest request) {

        String email = request.email().trim().toLowerCase(Locale.ROOT);

        // A mesma resposta é usada para e-mails cadastrados
        // e não cadastrados, evitando revelar a existência de contas.
        usuarioRepository.findByEmailIgnoreCase(email).ifPresent(usuario -> {

            String codigo = String.format(
                    "%06d",
                    random.nextInt(1_000_000)
            );

            RecuperacaoSenha recuperacao = recuperacaoRepository
                    .findByEmailIgnoreCase(email)
                    .orElseGet(RecuperacaoSenha::new);

            recuperacao.setEmail(email);
            recuperacao.setCodigoHash(passwordEncoder.encode(codigo));
            recuperacao.setTokenHash(null);
            recuperacao.setExpiraEm(Instant.now().plusSeconds(600));
            recuperacao.setTentativas(0);
            recuperacao.setValidado(false);
            recuperacao.setUsado(false);

            recuperacaoRepository.save(recuperacao);

            emailService.enviarCodigo(email, codigo);
        });

        return "Se o e-mail estiver cadastrado, você receberá um código de recuperação.";
    }

    public TokenRecuperacaoResponse validar(ValidarCodigoRequest request) {

        String email = request.email().trim().toLowerCase(Locale.ROOT);

        RecuperacaoSenha recuperacao = recuperacaoRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(this::codigoInvalido);

        if (recuperacao.isUsado()
                || recuperacao.isValidado()
                || recuperacao.getExpiraEm().isBefore(Instant.now())
                || recuperacao.getTentativas() >= 5) {
            throw codigoInvalido();
        }

        recuperacao.setTentativas(recuperacao.getTentativas() + 1);

        if (!passwordEncoder.matches(
                request.codigo(),
                recuperacao.getCodigoHash())) {

            recuperacaoRepository.save(recuperacao);
            throw codigoInvalido();
        }

        String token = gerarToken();

        recuperacao.setValidado(true);
        recuperacao.setTokenHash(passwordEncoder.encode(token));
        recuperacao.setExpiraEm(Instant.now().plusSeconds(600));

        recuperacaoRepository.save(recuperacao);

        return new TokenRecuperacaoResponse(
                "Código validado com sucesso.",
                token
        );
    }

    @Transactional
    public void redefinir(RedefinirSenhaRequest request) {

        String email = request.email().trim().toLowerCase(Locale.ROOT);

        RecuperacaoSenha recuperacao = recuperacaoRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(this::codigoInvalido);

        if (recuperacao.isUsado()
                || !recuperacao.isValidado()
                || recuperacao.getExpiraEm().isBefore(Instant.now())
                || recuperacao.getTokenHash() == null
                || !passwordEncoder.matches(
                request.token(),
                recuperacao.getTokenHash())) {
            throw codigoInvalido();
        }

        Usuario usuario = usuarioRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(this::codigoInvalido);

        usuario.setSenha(passwordEncoder.encode(request.novaSenha()));
        usuarioRepository.save(usuario);

        recuperacao.setUsado(true);
        recuperacao.setTokenHash(null);
        recuperacaoRepository.save(recuperacao);
    }

    private String gerarToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private ResponseStatusException codigoInvalido() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Código ou autorização inválidos, expirados ou já utilizados."
        );
    }
}