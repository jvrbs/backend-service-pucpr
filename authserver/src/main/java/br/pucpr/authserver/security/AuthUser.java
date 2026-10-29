package br.pucpr.authserver.security;

import java.security.Principal;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record AuthUser(Long id, String name, List<String> roles) implements Principal {
    public AuthUser { roles = List.copyOf(roles); }

    @Override
    public String getName() { return name; }

    public List<SimpleGrantedAuthority> authorities() {
        return roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
    }
}
