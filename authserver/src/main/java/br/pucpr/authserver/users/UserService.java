package br.pucpr.authserver.users;

import br.pucpr.authserver.exceptions.ApiException;
import br.pucpr.authserver.roles.Role;
import br.pucpr.authserver.security.AuthUser;
import br.pucpr.authserver.security.JwtService;
import br.pucpr.authserver.users.dto.*;
import br.pucpr.authserver.users.validation.PasswordPolicy;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final String dummyHash;

    public UserService(UserRepository repository, PasswordEncoder encoder, JwtService jwtService) {
        this.repository = repository;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.dummyHash = encoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw duplicateEmail();
        }
        User user = new User(request.email(), encoder.encode(request.password()), request.name());
        try {
            return UserResponse.from(repository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            // The database constraint also protects concurrent registrations.
            if (exception.getCause() instanceof org.hibernate.exception.ConstraintViolationException constraint
                    && constraint.getConstraintName() != null
                    && constraint.getConstraintName().toLowerCase(Locale.ROOT).contains("uk_user_email")) {
                throw duplicateEmail();
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = repository.findByEmail(request.email()).orElse(null);
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
        Sort.Direction direction;
        try {
            direction = Sort.Direction.valueOf(sortDir.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "sortDir deve ser ASC ou DESC.");
        }
        String normalizedRole = role == null ? null : role.trim().toUpperCase(Locale.ROOT);
        Sort sort = Sort.by(new Sort.Order(direction, "name").ignoreCase(), Sort.Order.asc("id"));
        return repository.findByRole(normalizedRole, sort).stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return repository.findById(id).map(UserResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }

    private ApiException duplicateEmail() {
        return new ApiException(HttpStatus.CONFLICT, "E-mail já cadastrado.");
    }
}
