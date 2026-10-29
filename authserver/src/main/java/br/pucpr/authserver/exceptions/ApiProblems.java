package br.pucpr.authserver.exceptions;

import java.net.URI;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

public final class ApiProblems {
    private ApiProblems() { }

    public static ProblemDetail create(HttpStatusCode status, String detail, String path) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setInstance(URI.create(path));
        return problem;
    }

    public static String detail(int status) {
        return switch (status) {
            case 400 -> "Requisição inválida.";
            case 401 -> "Autenticação necessária ou token inválido.";
            case 403 -> "Acesso negado.";
            case 404 -> "Recurso não encontrado.";
            case 409 -> "Conflito com um recurso existente.";
            case 422 -> "Um ou mais campos são inválidos.";
            case 405 -> "Método HTTP não permitido.";
            case 415 -> "Tipo de conteúdo não suportado.";
            default -> status >= 500 ? "Erro interno inesperado." : "Não foi possível atender à requisição.";
        };
    }
}
