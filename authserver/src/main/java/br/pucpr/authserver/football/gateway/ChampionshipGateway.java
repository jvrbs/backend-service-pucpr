package br.pucpr.authserver.football.gateway;

import br.pucpr.authserver.football.Championship;
import java.util.List;
import java.util.Optional;

public interface ChampionshipGateway {
    Optional<Championship> findById(long id);
    List<Championship> findByLeagueIdOrderBySeasonDesc(long leagueId);
    List<Championship> findByChampionIdOrderBySeasonDesc(long championId);
    Championship save(Championship title);
}
