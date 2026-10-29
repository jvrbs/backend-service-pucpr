package br.pucpr.authserver.users.dto;

import br.pucpr.authserver.users.Emails;
import br.pucpr.authserver.users.validation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record CreateUserRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @ValidPassword @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY) String password,
        @NotBlank @Size(max = 255) String name) {
    public CreateUserRequest {
        email = Emails.normalize(email);
        name = name == null ? null : name.trim();
    }

    @Override
    public String toString() { return "CreateUserRequest[password=REDACTED]"; }
}
