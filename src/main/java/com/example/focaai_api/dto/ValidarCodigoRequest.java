package com.example.focaai_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ValidarCodigoRequest(

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um e-mail válido.")
        String email,

        @NotBlank(message = "O código é obrigatório.")
        @Pattern(regexp = "\\d{6}", message = "O código deve ter 6 dígitos.")
        String codigo

) {
}