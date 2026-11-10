package com.example.focaai_api.repository;

import com.example.focaai_api.model.RecuperacaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecuperacaoSenhaRepository
        extends JpaRepository<RecuperacaoSenha, Long> {

    Optional<RecuperacaoSenha> findByEmailIgnoreCase(String email);

    void deleteByEmailIgnoreCase(String email);

}