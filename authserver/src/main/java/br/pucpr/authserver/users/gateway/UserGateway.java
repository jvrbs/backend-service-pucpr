package br.pucpr.authserver.users.gateway;

import br.pucpr.authserver.users.User;
import java.util.List;
import java.util.Optional;

public interface UserGateway {
    Optional<User> findById(long id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    User save(User user);
    List<User> findByRole(String role, boolean descending);
}
