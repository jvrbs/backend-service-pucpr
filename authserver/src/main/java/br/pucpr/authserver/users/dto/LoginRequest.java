package br.pucpr.authserver.users.dto;

import br.pucpr.authserver.users.Emails;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record LoginRequest(@NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY) String password) {
    public LoginRequest { email = Emails.normalize(email); }

    @Override
    public String toString() { return "LoginRequest[password=REDACTED]"; }
}
