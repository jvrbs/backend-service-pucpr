package br.pucpr.authserver.users;

import br.pucpr.authserver.roles.Role;
import jakarta.persistence.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "UserTable", uniqueConstraints = @UniqueConstraint(name = "uk_user_email", columnNames = "email"))
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false, length = 60)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "UserRole", joinColumns = @JoinColumn(name = "idUser"),
            inverseJoinColumns = @JoinColumn(name = "idRole"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"idUser", "idRole"}))
    private Set<Role> roles = new HashSet<>();

    protected User() { }

    public User(String email, String passwordHash, String name) {
        this.email = Emails.normalize(email);
        this.passwordHash = passwordHash;
        this.name = name.trim();
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getName() { return name; }
    public Set<Role> getRoles() { return Collections.unmodifiableSet(roles); }
    public void addRole(Role role) { roles.add(role); }
}
