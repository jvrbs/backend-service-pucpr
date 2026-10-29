package br.pucpr.authserver.users.dto;

public record LoginResponse(String token, UserResponse user) {
    @Override
    public String toString() { return "LoginResponse[token=REDACTED]"; }
}
