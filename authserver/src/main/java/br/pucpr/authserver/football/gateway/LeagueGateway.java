package br.pucpr.authserver.football.gateway;

import br.pucpr.authserver.football.League;
import java.util.List;
import java.util.Optional;

public interface LeagueGateway {
    long count();
    Optional<League> findById(long id);
    List<League> findAllByOrderByNameAsc();
    League save(League league);
}
