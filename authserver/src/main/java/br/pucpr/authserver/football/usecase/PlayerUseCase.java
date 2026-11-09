package br.pucpr.authserver.football.usecase;

import br.pucpr.authserver.football.Championship;
import br.pucpr.authserver.football.Player;
import br.pucpr.authserver.football.Team;
import br.pucpr.authserver.football.gateway.ChampionshipGateway;
import br.pucpr.authserver.football.gateway.PlayerGateway;
import br.pucpr.authserver.football.gateway.PlayerGateway.SortBy;

import br.pucpr.authserver.exceptions.ApiException;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import static br.pucpr.authserver.football.FootballDtos.*;

@Transactional(readOnly = true)
public class PlayerUseCase {
    private static final Logger log = LoggerFactory.getLogger(PlayerUseCase.class);
    private final PlayerGateway players;
    private final TeamUseCase teams;
    private final ChampionshipGateway championships;

    public PlayerUseCase(PlayerGateway players, TeamUseCase teams, ChampionshipGateway championships) {
        this.players = players;
        this.teams = teams;
        this.championships = championships;
    }

    public List<PlayerView> search(Long teamId, Long leagueId, String nationality, String sortBy, String sortDir, int limit) {
        SortBy field = switch (sortBy.toLowerCase(Locale.ROOT)) {
            case "name" -> SortBy.NAME;
            case "goals" -> SortBy.GOALS;
            case "appearances" -> SortBy.APPEARANCES;
            case "assists" -> SortBy.ASSISTS;
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "sortBy deve ser name, goals, appearances ou assists.");
        };
        boolean descending = switch (sortDir.toUpperCase(Locale.ROOT)) {
            case "ASC" -> false;
            case "DESC" -> true;
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "sortDir deve ser ASC ou DESC.");
        };
        String country = nationality == null ? null : nationality.trim();
        return players.search(teamId, leagueId, country, field, descending, limit).stream().map(PlayerView::of).toList();
    }

    public List<PlayerView> listByTeam(long teamId) {
        teams.requireTeam(teamId);
        return players.findByTeamIdOrderByNameAscIdAsc(teamId).stream().map(PlayerView::of).toList();
    }

    public PlayerView find(long id) {
        return PlayerView.of(requirePlayer(id));
    }

    @Transactional
    public PlayerView create(PlayerInput input) {
        Team team = teams.requireTeam(input.teamId());
        Player player = players.save(new Player(input.name(), input.age(), input.appearances(), input.goals(),
                input.assists(), input.marketValue(), input.nationality(), team));
        log.info("Criado jogador {} no time {}", player.getId(), team.getId());
        return PlayerView.of(player);
    }

    @Transactional
    public PlayerView update(long id, PlayerInput input) {
        Player player = requirePlayer(id);
        player.update(input.name(), input.age(), input.appearances(), input.goals(), input.assists(),
                input.marketValue(), input.nationality(), teams.requireTeam(input.teamId()));
        log.info("Atualizado jogador {}", id);
        return PlayerView.of(player);
    }

    @Transactional
    public PlayerView transfer(long id, long teamId) {
        Player player = requirePlayer(id);
        player.transfer(teams.requireTeam(teamId));
        log.info("Transferido jogador {} para time {}", id, teamId);
        return PlayerView.of(player);
    }

    @Transactional
    public PlayerView addTitle(long id, long titleId) {
        Player player = requirePlayer(id);
        player.addTitle(requireTitle(titleId));
        log.info("Associado título {} ao jogador {}", titleId, id);
        return PlayerView.of(player);
    }

    @Transactional
    public void removeTitle(long id, long titleId) {
        requirePlayer(id).removeTitle(requireTitle(titleId));
        log.info("Removido vínculo do título {} com jogador {}", titleId, id);
    }

    @Transactional
    public void delete(long id) {
        players.delete(requirePlayer(id));
        log.info("Excluído jogador {}", id);
    }

    private Player requirePlayer(long id) {
        return players.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Jogador não encontrado."));
    }

    private Championship requireTitle(long id) {
        return championships.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Título não encontrado."));
    }
}
