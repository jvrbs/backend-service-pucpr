package br.pucpr.authserver.football.gateway;

import br.pucpr.authserver.football.Player;
import java.util.List;
import java.util.Optional;

public interface PlayerGateway {
    enum SortBy { NAME, GOALS, APPEARANCES, ASSISTS }

    Optional<Player> findById(long id);
    List<Player> findByTeamIdOrderByNameAscIdAsc(long teamId);
    List<Player> search(Long teamId, Long leagueId, String nationality, SortBy sortBy, boolean descending, int limit);
    Player save(Player player);
    void delete(Player player);
}
