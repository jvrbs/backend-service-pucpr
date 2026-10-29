package br.pucpr.authserver.users;

import java.util.Locale;

public final class Emails {
    private Emails() { }

    public static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
