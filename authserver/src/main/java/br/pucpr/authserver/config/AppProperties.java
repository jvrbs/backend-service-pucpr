package br.pucpr.authserver.config;

import br.pucpr.authserver.users.Emails;
import br.pucpr.authserver.users.validation.PasswordPolicy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(@NotNull @Valid Security security, @NotNull @Valid Admin admin) {
    public record Security(@NotBlank String secret, @NotBlank String issuer,
                           @NotNull Duration tokenTtl, @NotNull Duration adminTokenTtl) {
        @AssertTrue(message = "APP_SECURITY_SECRET deve conter ao menos 32 bytes UTF-8")
        public boolean isSecretValid() {
            return secret != null && secret.getBytes(StandardCharsets.UTF_8).length >= 32;
        }

        @AssertTrue(message = "os prazos dos tokens devem ser positivos")
        public boolean isTtlValid() {
            return tokenTtl != null && tokenTtl.isPositive()
                    && adminTokenTtl != null && adminTokenTtl.isPositive();
        }

        @Override
        public String toString() { return "Security[secret=REDACTED]"; }
    }

    public record Admin(@NotBlank @Email @Size(max = 254) String email, String password) {
        public Admin { email = Emails.normalize(email); }

        // Validate a boolean so binding failures never print the rejected password.
        @AssertTrue(message = "APP_ADMIN_PASSWORD deve atender à política de senha")
        public boolean isPasswordValid() { return PasswordPolicy.isValid(password); }

        @Override
        public String toString() { return "Admin[password=REDACTED]"; }
    }
}
