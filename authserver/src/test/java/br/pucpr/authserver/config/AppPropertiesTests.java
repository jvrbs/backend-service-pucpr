package br.pucpr.authserver.config;

import jakarta.validation.Validation;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AppPropertiesTests {
    @Test
    void configurationRejectsMissingCredentialsWeakSecretAndInvalidTtl() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var config = new AppProperties(new AppProperties.Security("short", "", Duration.ZERO, Duration.ofHours(-1)),
                    new AppProperties.Admin("", "weak"));
            assertThat(validator.validate(config)).extracting(v -> v.getPropertyPath().toString())
                    .contains("security.secretValid", "security.issuer", "security.ttlValid", "admin.email", "admin.passwordValid");
        }
    }

    @Test
    void configurationCountsSecretBytesAndRedactsCredentials() {
        var config = new AppProperties(new AppProperties.Security("é".repeat(16), "authserver", Duration.ofHours(48), Duration.ofHours(1)),
                new AppProperties.Admin(" ADMIN@TEST.INVALID ", "TestOnly@123"));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(config)).isEmpty();
        }
        assertThat(config.admin().email()).isEqualTo("admin@test.invalid");
        assertThat(config.toString()).doesNotContain("TestOnly@123", "é".repeat(16));
    }
}
