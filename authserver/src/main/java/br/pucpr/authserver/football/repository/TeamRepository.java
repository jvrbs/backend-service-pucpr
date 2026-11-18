package br.pucpr.authserver.football.repository;

import br.pucpr.authserver.football.Team;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findAllByOrderByNameAscIdAsc();
    List<Team> findByLeagueIdOrderByNameAscIdAsc(Long leagueId);
}
