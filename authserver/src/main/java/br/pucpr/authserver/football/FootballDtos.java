package br.pucpr.authserver.football;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class FootballDtos {
    private FootballDtos() { }

    public record LeagueInput(@NotBlank @Size(max = 120) String name, @NotBlank @Size(max = 80) String country,
            @NotNull @PastOrPresent LocalDate foundedOn) { }

    public record TeamInput(@NotBlank @Size(max = 120) String name, @NotNull @PastOrPresent LocalDate foundedOn,
            @NotNull @PositiveOrZero Long supporters, @NotNull @Positive Long leagueId) { }

    public record PlayerInput(@NotBlank @Size(max = 120) String name, @NotNull @Min(1) @Max(120) Integer age,
            @NotNull @PositiveOrZero Integer appearances, @NotNull @PositiveOrZero Integer goals,
            @NotNull @PositiveOrZero Integer assists,
            @NotNull @PositiveOrZero @Digits(integer = 13, fraction = 2) BigDecimal marketValue,
            @NotBlank @Size(max = 80) String nationality, @NotNull @Positive Long teamId) { }

    public record ChampionshipInput(@NotBlank @Size(max = 20) String season, @NotNull @Positive Long championId) { }

    public record LeagueView(Long id, String name, String country, LocalDate foundedOn) {
        public static LeagueView of(League league) {
            return new LeagueView(league.getId(), league.getName(), league.getCountry(), league.getFoundedOn());
        }
    }

    public record TeamView(Long id, String name, LocalDate foundedOn, long supporters, Long leagueId) {
        public static TeamView of(Team team) {
            return new TeamView(team.getId(), team.getName(), team.getFoundedOn(), team.getSupporters(), team.getLeague().getId());
        }
    }

    public record PlayerView(Long id, String name, int age, int appearances, int goals, int assists,
            BigDecimal marketValue, String nationality, Long teamId, List<Long> titleIds) {
        public static PlayerView of(Player player) {
            return new PlayerView(player.getId(), player.getName(), player.getAge(), player.getAppearances(),
                    player.getGoals(), player.getAssists(), player.getMarketValue(), player.getNationality(),
                    player.getTeam().getId(), player.getTitles().stream().map(Championship::getId).sorted().toList());
        }
    }

    public record ChampionshipView(Long id, String season, Long leagueId, Long championId) {
        public static ChampionshipView of(Championship title) {
            return new ChampionshipView(title.getId(), title.getSeason(), title.getLeague().getId(), title.getChampion().getId());
        }
    }
}
