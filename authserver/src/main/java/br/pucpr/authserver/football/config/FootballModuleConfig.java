package br.pucpr.authserver.football.config;

import br.pucpr.authserver.football.FootballSeed;
import br.pucpr.authserver.football.gateway.ChampionshipGateway;
import br.pucpr.authserver.football.gateway.LeagueGateway;
import br.pucpr.authserver.football.gateway.PlayerGateway;
import br.pucpr.authserver.football.gateway.TeamGateway;
import br.pucpr.authserver.football.repository.ChampionshipRepository;
import br.pucpr.authserver.football.repository.H2ChampionshipGateway;
import br.pucpr.authserver.football.repository.H2LeagueGateway;
import br.pucpr.authserver.football.repository.H2PlayerGateway;
import br.pucpr.authserver.football.repository.H2TeamGateway;
import br.pucpr.authserver.football.repository.LeagueRepository;
import br.pucpr.authserver.football.repository.PlayerRepository;
import br.pucpr.authserver.football.repository.TeamRepository;
import br.pucpr.authserver.football.usecase.LeagueUseCase;
import br.pucpr.authserver.football.usecase.PlayerUseCase;
import br.pucpr.authserver.football.usecase.TeamUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
public class FootballModuleConfig {
    @Bean
    LeagueGateway leagueGateway(LeagueRepository repository) {
        return new H2LeagueGateway(repository);
    }

    @Bean
    TeamGateway teamGateway(TeamRepository repository) {
        return new H2TeamGateway(repository);
    }

    @Bean
    PlayerGateway playerGateway(PlayerRepository repository) {
        return new H2PlayerGateway(repository);
    }

    @Bean
    ChampionshipGateway championshipGateway(ChampionshipRepository repository) {
        return new H2ChampionshipGateway(repository);
    }

    @Bean
    TeamUseCase teamUseCase(TeamGateway teams, LeagueGateway leagues) {
        return new TeamUseCase(teams, leagues);
    }

    @Bean
    LeagueUseCase leagueUseCase(LeagueGateway leagues, ChampionshipGateway titles, TeamUseCase teams) {
        return new LeagueUseCase(leagues, titles, teams);
    }

    @Bean
    PlayerUseCase playerUseCase(PlayerGateway players, TeamUseCase teams, ChampionshipGateway titles) {
        return new PlayerUseCase(players, teams, titles);
    }

    @Bean
    @Profile({"dev", "football-demo"})
    FootballSeed footballSeed(ObjectMapper mapper, LeagueGateway leagues, TeamGateway teams, PlayerGateway players,
            ChampionshipGateway titles) {
        return new FootballSeed(mapper, leagues, teams, players, titles);
    }
}
