package com.example.focaai_api.service;

import com.example.focaai_api.dto.ValidarCodigoRequest;
import com.example.focaai_api.model.RecuperacaoSenha;
import com.example.focaai_api.model.Usuario;
import com.example.focaai_api.repository.RecuperacaoSenhaRepository;
import com.example.focaai_api.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecuperacaoSenhaServiceTest {

    private static final String EMAIL = "aluno@exemplo.com";
    private static final String CODIGO = "123456";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RecuperacaoSenhaRepository recuperacaoRepository;

    @Mock
    private ResendEmailService emailService;

    private RecuperacaoSenhaService service;
    private BCryptPasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        service = new RecuperacaoSenhaService(
                usuarioRepository,
                recuperacaoRepository,
                emailService
        );
        encoder = new BCryptPasswordEncoder();
    }

    @Test
    void validar_recuperacaoInexistente_retornaErro() {
        when(recuperacaoRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> service.validar(new ValidarCodigoRequest(EMAIL, CODIGO)));
        verify(recuperacaoRepository, never()).save(any());
    }

    @Test
    void validar_codigoJaUtilizado_retornaErro() {
        RecuperacaoSenha recuperacao = recuperacaoValida();
        recuperacao.setUsado(true);
        prepararRecuperacao(recuperacao);

        assertThrows(ResponseStatusException.class,
                () -> service.validar(new ValidarCodigoRequest(EMAIL, CODIGO)));
        verify(recuperacaoRepository, never()).save(any());
    }

    @Test
    void validar_codigoJaValidado_retornaErro() {
        RecuperacaoSenha recuperacao = recuperacaoValida();
        recuperacao.setValidado(true);
        prepararRecuperacao(recuperacao);

        assertThrows(ResponseStatusException.class,
                () -> service.validar(new ValidarCodigoRequest(EMAIL, CODIGO)));
        verify(recuperacaoRepository, never()).save(any());
    }

    @Test
    void validar_codigoExpirado_retornaErro() {
        RecuperacaoSenha recuperacao = recuperacaoValida();
        recuperacao.setExpiraEm(Instant.now().minusSeconds(1));
        prepararRecuperacao(recuperacao);

        assertThrows(ResponseStatusException.class,
                () -> service.validar(new ValidarCodigoRequest(EMAIL, CODIGO)));
        verify(recuperacaoRepository, never()).save(any());
    }

    @Test
    void validar_limiteDeTentativasAtingido_retornaErro() {
        RecuperacaoSenha recuperacao = recuperacaoValida();
        recuperacao.setTentativas(5);
        prepararRecuperacao(recuperacao);

        assertThrows(ResponseStatusException.class,
                () -> service.validar(new ValidarCodigoRequest(EMAIL, CODIGO)));
        verify(recuperacaoRepository, never()).save(any());
    }

    @Test
    void validar_codigoIncorreto_incrementaTentativasERetornaErro() {
        RecuperacaoSenha recuperacao = recuperacaoValida();
        prepararRecuperacao(recuperacao);

        assertThrows(ResponseStatusException.class,
                () -> service.validar(new ValidarCodigoRequest(EMAIL, "999999")));

        assertEquals(1, recuperacao.getTentativas());
        verify(recuperacaoRepository).save(recuperacao);
    }

    @Test
    void validar_codigoCorreto_geraTokenEValidaRecuperacao() {
        RecuperacaoSenha recuperacao = recuperacaoValida();
        prepararRecuperacao(recuperacao);

        var resposta = service.validar(new ValidarCodigoRequest(EMAIL, CODIGO));

        assertNotNull(resposta);
        assertNotNull(resposta.token());
        assertFalse(resposta.token().isBlank());
        assertTrue(recuperacao.isValidado());
        assertEquals(1, recuperacao.getTentativas());
        assertNotNull(recuperacao.getTokenHash());
        assertTrue(encoder.matches(resposta.token(), recuperacao.getTokenHash()));
        assertTrue(recuperacao.getExpiraEm().isAfter(Instant.now()));
        verify(recuperacaoRepository).save(recuperacao);
    }

    private RecuperacaoSenha recuperacaoValida() {
        RecuperacaoSenha recuperacao = new RecuperacaoSenha();
        recuperacao.setEmail(EMAIL);
        recuperacao.setCodigoHash(encoder.encode(CODIGO));
        recuperacao.setExpiraEm(Instant.now().plusSeconds(600));
        recuperacao.setTentativas(0);
        recuperacao.setValidado(false);
        recuperacao.setUsado(false);
        return recuperacao;
    }

    private void prepararRecuperacao(RecuperacaoSenha recuperacao) {
        when(recuperacaoRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.of(recuperacao));
    }
}
