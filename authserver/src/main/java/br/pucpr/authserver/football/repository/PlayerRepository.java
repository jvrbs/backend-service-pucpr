package br.pucpr.authserver.football.repository;

import br.pucpr.authserver.football.Player;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    List<Player> findByTeamIdOrderByNameAscIdAsc(Long teamId);

    @Query("""
            select p from FootballPlayer p join fetch p.team t
            where (:teamId is null or t.id = :teamId)
              and (:leagueId is null or t.league.id = :leagueId)
              and (:nationality is null or lower(p.nationality) = lower(:nationality))
            """)
    List<Player> search(@Param("teamId") Long teamId, @Param("leagueId") Long leagueId,
            @Param("nationality") String nationality, Pageable pageable);
}
