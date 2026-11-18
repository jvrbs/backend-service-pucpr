package br.pucpr.authserver.users.repository;

import br.pucpr.authserver.users.gateway.UserGateway;
import br.pucpr.authserver.users.User;
import java.util.List;
import java.util.Optional;
import br.pucpr.authserver.exceptions.PersistenceConflictException;
import java.util.Locale;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;

public class H2UserGateway implements UserGateway {
    private final UserRepository repository;

    public H2UserGateway(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<User> findById(long id) {
        return repository.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        try {
            return repository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            if (exception.getCause() instanceof ConstraintViolationException constraint
                    && constraint.getConstraintName() != null
                    && constraint.getConstraintName().toLowerCase(Locale.ROOT).contains("uk_user_email")) {
                throw new PersistenceConflictException(exception);
            }
            throw exception;
        }
    }

    @Override
    public List<User> findByRole(String role, boolean descending) {
        Sort.Direction direction = descending ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(new Sort.Order(direction, "name").ignoreCase(), Sort.Order.asc("id"));
        return repository.findByRole(role, sort);
    }
}
