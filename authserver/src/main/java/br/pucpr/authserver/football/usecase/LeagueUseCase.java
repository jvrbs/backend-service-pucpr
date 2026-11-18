package br.pucpr.authserver.football.usecase;

import br.pucpr.authserver.football.League;
import br.pucpr.authserver.football.Championship;
import br.pucpr.authserver.football.Team;
import br.pucpr.authserver.football.gateway.LeagueGateway;
import br.pucpr.authserver.football.gateway.ChampionshipGateway;

import br.pucpr.authserver.exceptions.ApiException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.pucpr.authserver.exceptions.PersistenceConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import static br.pucpr.authserver.football.FootballDtos.*;

@Transactional(readOnly = true)
public class LeagueUseCase {
    private static final Logger log = LoggerFactory.getLogger(LeagueUseCase.class);
    private final LeagueGateway leagues;
    private final ChampionshipGateway championships;
    private final TeamUseCase teams;

    public LeagueUseCase(LeagueGateway leagues, ChampionshipGateway championships, TeamUseCase teams) {
        this.leagues = leagues;
        this.championships = championships;
        this.teams = teams;
    }

    public List<LeagueView> list() {
        return leagues.findAllByOrderByNameAsc().stream().map(LeagueView::of).toList();
    }

    public LeagueView find(long id) {
        return LeagueView.of(teams.requireLeague(id));
    }

    @Transactional
    public LeagueView create(LeagueInput input) {
        try {
            League league = leagues.save(new League(input.name(), input.country(), input.foundedOn()));
            log.info("Criada liga {}", league.getId());
            return LeagueView.of(league);
        } catch (PersistenceConflictException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "Já existe uma liga com esse nome.");
        }
    }

    public List<ChampionshipView> listLeagueTitles(long leagueId) {
        teams.requireLeague(leagueId);
        return championships.findByLeagueIdOrderBySeasonDesc(leagueId).stream().map(ChampionshipView::of).toList();
    }

    public List<ChampionshipView> listTeamTitles(long teamId) {
        teams.requireTeam(teamId);
        return championships.findByChampionIdOrderBySeasonDesc(teamId).stream().map(ChampionshipView::of).toList();
    }

    @Transactional
    public ChampionshipView createTitle(long leagueId, ChampionshipInput input) {
        League league = teams.requireLeague(leagueId);
        Team champion = teams.requireTeam(input.championId());
        if (!champion.getLeague().getId().equals(league.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "O campeão deve pertencer à liga no registro do título.");
        }
        try {
            Championship title = championships.save(new Championship(input.season(), league, champion));
            log.info("Registrado título {} da liga {} para time {}", title.getId(), leagueId, champion.getId());
            return ChampionshipView.of(title);
        } catch (PersistenceConflictException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "Já existe um campeão registrado para essa temporada da liga.");
        }
    }
}
