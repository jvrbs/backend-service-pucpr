package br.pucpr.authserver.football.gateway;

import br.pucpr.authserver.football.Team;
import java.util.List;
import java.util.Optional;

public interface TeamGateway {
    Optional<Team> findById(long id);
    List<Team> findAllByOrderByNameAscIdAsc();
    List<Team> findByLeagueIdOrderByNameAscIdAsc(long leagueId);
    Team save(Team team);
    void delete(Team team);
}
