package br.pucpr.authserver.exceptions;

public class PersistenceConflictException extends RuntimeException {
    public PersistenceConflictException(Throwable cause) {
        super(cause);
    }
}
