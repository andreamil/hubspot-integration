package com.meetime.hubspot.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ContactPropertiesDTO(
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email deve ser válido")
        String email,
        String firstname,
        String lastname,
        String phone
) {}
