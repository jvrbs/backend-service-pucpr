package br.pucpr.authserver.users.usecase;

import br.pucpr.authserver.exceptions.PersistenceConflictException;
import br.pucpr.authserver.users.User;
import br.pucpr.authserver.users.gateway.UserGateway;

import br.pucpr.authserver.exceptions.ApiException;
import br.pucpr.authserver.roles.Role;
import br.pucpr.authserver.security.AuthUser;
import br.pucpr.authserver.security.JwtService;
import br.pucpr.authserver.users.dto.*;
import br.pucpr.authserver.users.validation.PasswordPolicy;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

public class UserUseCase {
    private final UserGateway users;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final String dummyHash;

    public UserUseCase(UserGateway users, PasswordEncoder encoder, JwtService jwtService) {
        this.users = users;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.dummyHash = encoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (users.existsByEmail(request.email())) {
            throw duplicateEmail();
        }
        User user = new User(request.email(), encoder.encode(request.password()), request.name());
        try {
            return UserResponse.from(users.save(user));
        } catch (PersistenceConflictException exception) {
            throw duplicateEmail();
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = users.findByEmail(request.email()).orElse(null);
        boolean withinLimit = PasswordPolicy.withinBcryptLimit(request.password());
        String candidate = withinLimit ? request.password() : "";
        boolean matches = encoder.matches(candidate, user == null ? dummyHash : user.getPasswordHash());
        if (user == null || !withinLimit || !matches) {
            throw new BadCredentialsException("Credenciais inválidas.");
        }
        List<String> roles = user.getRoles().stream().map(Role::getName).sorted().toList();
        return new LoginResponse(jwtService.issue(new AuthUser(user.getId(), user.getName(), roles)),
                UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list(String sortDir, String role) {
        boolean descending = switch (sortDir.toUpperCase(Locale.ROOT)) {
            case "ASC" -> false;
            case "DESC" -> true;
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "sortDir deve ser ASC ou DESC.");
        };
        String normalizedRole = role == null ? null : role.trim().toUpperCase(Locale.ROOT);
        return users.findByRole(normalizedRole, descending).stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return users.findById(id).map(UserResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }

    private ApiException duplicateEmail() {
        return new ApiException(HttpStatus.CONFLICT, "E-mail já cadastrado.");
    }
}
