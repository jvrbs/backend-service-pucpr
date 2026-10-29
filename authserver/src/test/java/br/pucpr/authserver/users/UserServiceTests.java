package br.pucpr.authserver.users;

import br.pucpr.authserver.exceptions.ApiException;
import br.pucpr.authserver.exceptions.ApiExceptionHandler;
import br.pucpr.authserver.security.JwtService;
import br.pucpr.authserver.users.dto.CreateUserRequest;
import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserServiceTests {
    @Test
    void concurrentDuplicateIsTranslatedTo409() {
        var repository = mock(UserRepository.class);
        var encoder = mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenReturn("hash");
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("constraint",
                new ConstraintViolationException("duplicate", new SQLException(), "uk_user_email")));
        var service = new UserService(repository, encoder, mock(JwtService.class));
        assertThatThrownBy(() -> service.create(new CreateUserRequest("a@test.invalid", "Password@1", "Ana")))
                .isInstanceOfSatisfying(ApiException.class, ex ->
                        org.assertj.core.api.Assertions.assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void unexpectedFailureReturnsGenericProblemWithoutInternalDetails() throws Exception {
        var service = mock(UserService.class);
        when(service.findById(1L)).thenThrow(new IllegalStateException("secret-database-details"));
        var mvc = MockMvcBuilders.standaloneSetup(new UserController(service))
                .setControllerAdvice(new ApiExceptionHandler()).build();
        mvc.perform(get("/users/1")).andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(content().string(not(containsString("secret-database-details"))))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
