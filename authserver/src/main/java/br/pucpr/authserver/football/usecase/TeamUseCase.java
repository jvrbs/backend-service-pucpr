package br.pucpr.authserver.football.usecase;

import br.pucpr.authserver.football.League;
import br.pucpr.authserver.football.Team;
import br.pucpr.authserver.football.gateway.LeagueGateway;
import br.pucpr.authserver.football.gateway.TeamGateway;

import br.pucpr.authserver.exceptions.ApiException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.pucpr.authserver.exceptions.PersistenceConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import static br.pucpr.authserver.football.FootballDtos.*;

@Transactional(readOnly = true)
public class TeamUseCase {
    private static final Logger log = LoggerFactory.getLogger(TeamUseCase.class);
    private final TeamGateway teams;
    private final LeagueGateway leagues;

    public TeamUseCase(TeamGateway teams, LeagueGateway leagues) {
        this.teams = teams;
        this.leagues = leagues;
    }

    public List<TeamView> list() {
        return teams.findAllByOrderByNameAscIdAsc().stream().map(TeamView::of).toList();
    }

    public List<TeamView> listByLeague(long leagueId) {
        requireLeague(leagueId);
        return teams.findByLeagueIdOrderByNameAscIdAsc(leagueId).stream().map(TeamView::of).toList();
    }

    public TeamView find(long id) {
        return TeamView.of(requireTeam(id));
    }

    @Transactional
    public TeamView create(TeamInput input) {
        Team team = teams.save(new Team(input.name(), input.foundedOn(), input.supporters(), requireLeague(input.leagueId())));
        log.info("Criado time {}", team.getId());
        return TeamView.of(team);
    }

    @Transactional
    public TeamView update(long id, TeamInput input) {
        Team team = requireTeam(id);
        team.update(input.name(), input.foundedOn(), input.supporters(), requireLeague(input.leagueId()));
        log.info("Atualizado time {}", id);
        return TeamView.of(team);
    }

    @Transactional
    public TeamView joinLeague(long id, long leagueId) {
        Team team = requireTeam(id);
        team.join(requireLeague(leagueId));
        log.info("Associado time {} à liga {}", id, leagueId);
        return TeamView.of(team);
    }

    @Transactional
    public void delete(long id) {
        Team team = requireTeam(id);
        try {
            teams.delete(team);
        } catch (PersistenceConflictException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "O time está vinculado a jogadores ou títulos.");
        }
        log.info("Excluído time {}", id);
    }

    Team requireTeam(long id) {
        return teams.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Time não encontrado."));
    }

    League requireLeague(long id) {
        return leagues.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Liga não encontrada."));
    }
}
