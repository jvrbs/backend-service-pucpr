package br.pucpr.authserver.football.repository;

import br.pucpr.authserver.football.gateway.PlayerGateway;
import br.pucpr.authserver.football.Player;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public class H2PlayerGateway implements PlayerGateway {
    private final PlayerRepository repository;

    public H2PlayerGateway(PlayerRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Player> findById(long id) {
        return repository.findById(id);
    }

    @Override
    public List<Player> findByTeamIdOrderByNameAscIdAsc(long teamId) {
        return repository.findByTeamIdOrderByNameAscIdAsc(teamId);
    }

    @Override
    public List<Player> search(Long teamId, Long leagueId, String nationality, SortBy sortBy, boolean descending, int limit) {
        Sort.Direction direction = descending ? Sort.Direction.DESC : Sort.Direction.ASC;
        String property = switch (sortBy) {
            case NAME -> "name";
            case GOALS -> "goals";
            case APPEARANCES -> "appearances";
            case ASSISTS -> "assists";
        };
        Sort.Order order = new Sort.Order(direction, property);
        if (sortBy == SortBy.NAME) {
            order = order.ignoreCase();
        }
        var page = PageRequest.of(0, limit, Sort.by(order, Sort.Order.asc("id")));
        return repository.search(teamId, leagueId, nationality, page);
    }

    @Override
    public Player save(Player player) {
        return repository.save(player);
    }

    @Override
    public void delete(Player player) {
        repository.delete(player);
    }
}
