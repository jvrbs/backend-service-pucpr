package br.pucpr.authserver.football.repository;

import br.pucpr.authserver.football.League;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeagueRepository extends JpaRepository<League, Long> {
    List<League> findAllByOrderByNameAsc();
}
