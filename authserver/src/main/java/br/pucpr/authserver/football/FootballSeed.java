package br.pucpr.authserver.football;

import br.pucpr.authserver.football.gateway.LeagueGateway;
import br.pucpr.authserver.football.gateway.TeamGateway;
import br.pucpr.authserver.football.gateway.PlayerGateway;
import br.pucpr.authserver.football.gateway.ChampionshipGateway;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

public class FootballSeed implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(FootballSeed.class);
    private final ObjectMapper mapper;
    private final LeagueGateway leagues;
    private final TeamGateway teams;
    private final PlayerGateway players;
    private final ChampionshipGateway championships;

    public FootballSeed(ObjectMapper mapper, LeagueGateway leagues, TeamGateway teams, PlayerGateway players,
            ChampionshipGateway championships) {
        this.mapper = mapper;
        this.leagues = leagues;
        this.teams = teams;
        this.players = players;
        this.championships = championships;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments arguments) throws Exception {
        if (leagues.count() > 0) {
            return;
        }
        try (var input = new ClassPathResource("football-demo.json").getInputStream()) {
            for (var leagueData : mapper.readValue(input, LeagueSeed[].class)) {
                League league = leagues.save(new League(leagueData.name(), leagueData.country(), leagueData.foundedOn()));
                for (var teamData : leagueData.teams()) {
                    Team team = teams.save(new Team(teamData.name(), teamData.foundedOn(), teamData.supporters(), league));
                    Championship title = teamData.titleSeason() == null ? null
                            : championships.save(new Championship(teamData.titleSeason(), league, team));
                    for (var playerData : teamData.players()) {
                        Player player = new Player(playerData.name(), playerData.age(), playerData.appearances(),
                                playerData.goals(), playerData.assists(), playerData.marketValue(), playerData.nationality(), team);
                        if (playerData.champion()) {
                            if (title == null) {
                                throw new IllegalStateException("Jogador campeão cadastrado sem título para o time.");
                            }
                            player.addTitle(title);
                        }
                        players.save(player);
                    }
                }
            }
        }
        log.info("Carga de demonstração de futebol aplicada");
    }

    private record LeagueSeed(String name, String country, LocalDate foundedOn, List<TeamSeed> teams) { }
    private record TeamSeed(String name, LocalDate foundedOn, long supporters, String titleSeason, List<PlayerSeed> players) { }
    private record PlayerSeed(String name, int age, int appearances, int goals, int assists, BigDecimal marketValue,
            String nationality, boolean champion) { }
}
