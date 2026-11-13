package com.example.focaai_api.service;

import com.example.focaai_api.dto.CadastroUsuarioRequest;
import com.example.focaai_api.dto.AlterarSenhaRequest;
import com.example.focaai_api.dto.ExcluirContaRequest;
import com.example.focaai_api.dto.LoginRequest;
import com.example.focaai_api.dto.UsuarioResponse;
import com.example.focaai_api.model.Usuario;
import com.example.focaai_api.repository.RecuperacaoSenhaRepository;
import com.example.focaai_api.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RecuperacaoSenhaRepository recuperacaoRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            RecuperacaoSenhaRepository recuperacaoRepository) {

        this.usuarioRepository = usuarioRepository;
        this.recuperacaoRepository = recuperacaoRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public UsuarioResponse cadastrar(CadastroUsuarioRequest request) {

        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Este e-mail já está cadastrado."
            );
        }

        String senhaCriptografada =
                passwordEncoder.encode(request.senha());

        Usuario usuario = new Usuario(
                request.nome().trim(),
                email,
                senhaCriptografada
        );

        Usuario usuarioSalvo = usuarioRepository.save(usuario);

        return new UsuarioResponse(
                usuarioSalvo.getId(),
                usuarioSalvo.getNome(),
                usuarioSalvo.getEmail()
        );
    }

    public UsuarioResponse login(LoginRequest request) {

        String email = request.email().trim().toLowerCase(Locale.ROOT);

        Usuario usuario = usuarioRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "E-mail ou senha inválidos."
                ));

        boolean senhaCorreta = passwordEncoder.matches(
                request.senha(),
                usuario.getSenha()
        );

        if (!senhaCorreta) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "E-mail ou senha inválidos."
            );
        }

        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail()
        );
    }

    @Transactional
    public void alterarSenha(AlterarSenhaRequest request) {

        String email = request.email().trim().toLowerCase(Locale.ROOT);

        Usuario usuario = usuarioRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "E-mail ou senha atual inválidos."
                ));

        boolean senhaAtualCorreta = passwordEncoder.matches(
                request.senhaAtual(),
                usuario.getSenha()
        );

        if (!senhaAtualCorreta) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "E-mail ou senha atual inválidos."
            );
        }

        if (passwordEncoder.matches(request.novaSenha(), usuario.getSenha())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A nova senha deve ser diferente da senha atual."
            );
        }

        usuario.setSenha(passwordEncoder.encode(request.novaSenha()));
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void excluirConta(ExcluirContaRequest request) {

        String email = request.email().trim().toLowerCase(Locale.ROOT);

        Usuario usuario = usuarioRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "E-mail ou senha inválidos."
                ));

        boolean senhaCorreta = passwordEncoder.matches(
                request.senha(),
                usuario.getSenha()
        );

        if (!senhaCorreta) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "E-mail ou senha inválidos."
            );
        }

        // Exclui primeiro os dados de recuperação de senha.
        recuperacaoRepository.deleteByEmailIgnoreCase(email);

        // Depois exclui a conta do usuário.
        usuarioRepository.delete(usuario);
    }
}