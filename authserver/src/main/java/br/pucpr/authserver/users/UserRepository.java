package br.pucpr.authserver.users;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("select u from User u where :role is null or exists "
            + "(select r.id from u.roles r where r.name = :role)")
    List<User> findByRole(@Param("role") String role, Sort sort);
}
