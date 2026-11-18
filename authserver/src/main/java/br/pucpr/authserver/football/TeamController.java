package br.pucpr.authserver.football;

import br.pucpr.authserver.football.usecase.TeamUseCase;
import br.pucpr.authserver.football.usecase.PlayerUseCase;
import br.pucpr.authserver.football.usecase.LeagueUseCase;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static br.pucpr.authserver.football.FootballDtos.*;

@RestController
@RequestMapping("/football/teams")
public class TeamController {
    private final TeamUseCase teams;
    private final PlayerUseCase players;
    private final LeagueUseCase leagues;

    public TeamController(TeamUseCase teams, PlayerUseCase players, LeagueUseCase leagues) {
        this.teams = teams;
        this.players = players;
        this.leagues = leagues;
    }

    @GetMapping
    public List<TeamView> list() { return teams.list(); }

    @GetMapping("/{id}")
    public TeamView find(@PathVariable @Positive long id) { return teams.find(id); }

    @GetMapping("/{id}/players")
    public List<PlayerView> players(@PathVariable @Positive long id) { return players.listByTeam(id); }

    @GetMapping("/{id}/titles")
    public List<ChampionshipView> titles(@PathVariable @Positive long id) { return leagues.listTeamTitles(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    public TeamView create(@Valid @RequestBody TeamInput input) { return teams.create(input); }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public TeamView update(@PathVariable @Positive long id, @Valid @RequestBody TeamInput input) {
        return teams.update(id, input);
    }

    @PutMapping("/{id}/league/{leagueId}")
    @SecurityRequirement(name = "bearerAuth")
    public TeamView joinLeague(@PathVariable @Positive long id, @PathVariable @Positive long leagueId) {
        return teams.joinLeague(id, leagueId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    public void delete(@PathVariable @Positive long id) { teams.delete(id); }
}
