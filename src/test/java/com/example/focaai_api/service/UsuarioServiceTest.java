package com.example.focaai_api.service;

import com.example.focaai_api.dto.AlterarSenhaRequest;
import com.example.focaai_api.dto.CadastroUsuarioRequest;
import com.example.focaai_api.dto.ExcluirContaRequest;
import com.example.focaai_api.dto.LoginRequest;
import com.example.focaai_api.model.Usuario;
import com.example.focaai_api.repository.RecuperacaoSenhaRepository;
import com.example.focaai_api.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final String EMAIL = "aluno@exemplo.com";
    private static final String SENHA = "SenhaForte123";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RecuperacaoSenhaRepository recuperacaoRepository;

    private UsuarioService service;
    private BCryptPasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(usuarioRepository, recuperacaoRepository);
        encoder = new BCryptPasswordEncoder();
    }

    @Test
    void cadastrar_emailNovo_criaUsuarioComSenhaCriptografada() {
        when(usuarioRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.cadastrar(
                new CadastroUsuarioRequest("Aluno Teste", EMAIL, SENHA)
        );

        assertEquals("Aluno Teste", resposta.nome());
        assertEquals(EMAIL, resposta.email());
        verify(usuarioRepository).save(argThat(usuario ->
                encoder.matches(SENHA, usuario.getSenha())
                        && !usuario.getSenha().equals(SENHA)
        ));
    }

    @Test
    void cadastrar_emailJaCadastrado_retornaConflito() {
        when(usuarioRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> service.cadastrar(
                        new CadastroUsuarioRequest("Aluno", EMAIL, SENHA)
                )
        );

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void login_credenciaisCorretas_retornaUsuario() {
        Usuario usuario = usuarioComSenha();
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(usuario));

        var resposta = service.login(new LoginRequest(EMAIL, SENHA));

        assertEquals(usuario.getNome(), resposta.nome());
        assertEquals(EMAIL, resposta.email());
    }

    @Test
    void login_senhaIncorreta_retornaNaoAutorizado() {
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(usuarioComSenha()));

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> service.login(new LoginRequest(EMAIL, "SenhaErrada"))
        );

        assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatusCode());
    }

    @Test
    void login_usuarioInexistente_retornaNaoAutorizado() {
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> service.login(new LoginRequest(EMAIL, SENHA))
        );

        assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatusCode());
    }

    @Test
    void alterarSenha_senhaAtualIncorreta_retornaNaoAutorizado() {
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(usuarioComSenha()));

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> service.alterarSenha(
                        new AlterarSenhaRequest(EMAIL, "SenhaErrada", "NovaSenha123")
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatusCode());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void alterarSenha_novaSenhaIgualAtual_retornaErro() {
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(usuarioComSenha()));

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> service.alterarSenha(
                        new AlterarSenhaRequest(EMAIL, SENHA, SENHA)
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void alterarSenha_dadosCorretos_salvaNovaSenhaCriptografada() {
        Usuario usuario = usuarioComSenha();
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(usuario));

        service.alterarSenha(
                new AlterarSenhaRequest(EMAIL, SENHA, "NovaSenha123")
        );

        assertTrue(encoder.matches("NovaSenha123", usuario.getSenha()));
        assertFalse(encoder.matches(SENHA, usuario.getSenha()));
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void excluirConta_senhaCorreta_excluiDadosEUsuario() {
        Usuario usuario = usuarioComSenha();
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(usuario));

        service.excluirConta(new ExcluirContaRequest(EMAIL, SENHA));

        verify(recuperacaoRepository).deleteByEmailIgnoreCase(EMAIL);
        verify(usuarioRepository).delete(usuario);
    }

    @Test
    void excluirConta_senhaIncorreta_naoExcluiUsuario() {
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(usuarioComSenha()));

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> service.excluirConta(
                        new ExcluirContaRequest(EMAIL, "SenhaErrada")
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatusCode());
        verify(recuperacaoRepository, never()).deleteByEmailIgnoreCase(any());
        verify(usuarioRepository, never()).delete(any(Usuario.class));
    }

    private Usuario usuarioComSenha() {
        return new Usuario(
                "Aluno Teste",
                EMAIL,
                encoder.encode(SENHA)
        );
    }
}
