package br.pucpr.authserver.football;

import br.pucpr.authserver.football.usecase.LeagueUseCase;
import br.pucpr.authserver.football.usecase.TeamUseCase;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static br.pucpr.authserver.football.FootballDtos.*;

@RestController
@RequestMapping("/football/leagues")
public class LeagueController {
    private final LeagueUseCase leagues;
    private final TeamUseCase teams;

    public LeagueController(LeagueUseCase leagues, TeamUseCase teams) {
        this.leagues = leagues;
        this.teams = teams;
    }

    @GetMapping
    public List<LeagueView> list() { return leagues.list(); }

    @GetMapping("/{id}")
    public LeagueView find(@PathVariable @Positive long id) { return leagues.find(id); }

    @GetMapping("/{id}/teams")
    public List<TeamView> teams(@PathVariable @Positive long id) { return teams.listByLeague(id); }

    @GetMapping("/{id}/titles")
    public List<ChampionshipView> titles(@PathVariable @Positive long id) { return leagues.listLeagueTitles(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    public LeagueView create(@Valid @RequestBody LeagueInput input) { return leagues.create(input); }

    @PostMapping("/{id}/titles")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    public ChampionshipView createTitle(@PathVariable @Positive long id, @Valid @RequestBody ChampionshipInput input) {
        return leagues.createTitle(id, input);
    }
}
