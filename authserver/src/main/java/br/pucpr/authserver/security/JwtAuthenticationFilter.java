package br.pucpr.authserver.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final ProblemAuthenticationEntryPoint entryPoint;

    public JwtAuthenticationFilter(JwtService jwtService, ProblemAuthenticationEntryPoint entryPoint) {
        this.jwtService = jwtService;
        this.entryPoint = entryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null) {
            try {
                if (!header.regionMatches(true, 0, "Bearer ", 0, 7) || header.substring(7).isBlank()) {
                    throw new BadCredentialsException("Token inválido.");
                }
                AuthUser user = jwtService.parse(header.substring(7));
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.authorities()));
                SecurityContextHolder.setContext(context);
            } catch (JwtException | IllegalArgumentException | BadCredentialsException exception) {
                SecurityContextHolder.clearContext();
                entryPoint.commence(request, response, new BadCredentialsException("Token inválido."));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
