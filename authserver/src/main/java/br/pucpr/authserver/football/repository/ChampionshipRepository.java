package br.pucpr.authserver.football.repository;

import br.pucpr.authserver.football.Championship;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChampionshipRepository extends JpaRepository<Championship, Long> {
    List<Championship> findByLeagueIdOrderBySeasonDesc(Long leagueId);
    List<Championship> findByChampionIdOrderBySeasonDesc(Long championId);
}
