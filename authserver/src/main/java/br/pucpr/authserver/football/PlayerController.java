package br.pucpr.authserver.football;

import br.pucpr.authserver.football.usecase.PlayerUseCase;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static br.pucpr.authserver.football.FootballDtos.*;

@RestController
@RequestMapping("/football/players")
public class PlayerController {
    private final PlayerUseCase players;

    public PlayerController(PlayerUseCase players) { this.players = players; }

    @GetMapping
    public List<PlayerView> search(@RequestParam(required = false) @Positive Long teamId,
            @RequestParam(required = false) @Positive Long leagueId,
            @RequestParam(required = false) String nationality,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int limit) {
        return players.search(teamId, leagueId, nationality, sortBy, sortDir, limit);
    }

    @GetMapping("/{id}")
    public PlayerView find(@PathVariable @Positive long id) { return players.find(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    public PlayerView create(@Valid @RequestBody PlayerInput input) { return players.create(input); }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public PlayerView update(@PathVariable @Positive long id, @Valid @RequestBody PlayerInput input) {
        return players.update(id, input);
    }

    @PutMapping("/{id}/team/{teamId}")
    @SecurityRequirement(name = "bearerAuth")
    public PlayerView transfer(@PathVariable @Positive long id, @PathVariable @Positive long teamId) {
        return players.transfer(id, teamId);
    }

    @PutMapping("/{id}/titles/{titleId}")
    @SecurityRequirement(name = "bearerAuth")
    public PlayerView addTitle(@PathVariable @Positive long id, @PathVariable @Positive long titleId) {
        return players.addTitle(id, titleId);
    }

    @DeleteMapping("/{id}/titles/{titleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    public void removeTitle(@PathVariable @Positive long id, @PathVariable @Positive long titleId) {
        players.removeTitle(id, titleId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    public void delete(@PathVariable @Positive long id) { players.delete(id); }
}
