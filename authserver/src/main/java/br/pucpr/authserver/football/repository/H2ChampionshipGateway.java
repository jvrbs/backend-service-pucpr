package br.pucpr.authserver.football.repository;

import br.pucpr.authserver.football.gateway.ChampionshipGateway;
import br.pucpr.authserver.football.Championship;
import java.util.List;
import java.util.Optional;
import br.pucpr.authserver.exceptions.PersistenceConflictException;
import org.springframework.dao.DataIntegrityViolationException;

public class H2ChampionshipGateway implements ChampionshipGateway {
    private final ChampionshipRepository repository;

    public H2ChampionshipGateway(ChampionshipRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Championship> findById(long id) {
        return repository.findById(id);
    }

    @Override
    public List<Championship> findByLeagueIdOrderBySeasonDesc(long leagueId) {
        return repository.findByLeagueIdOrderBySeasonDesc(leagueId);
    }

    @Override
    public List<Championship> findByChampionIdOrderBySeasonDesc(long championId) {
        return repository.findByChampionIdOrderBySeasonDesc(championId);
    }

    @Override
    public Championship save(Championship title) {
        try {
            return repository.saveAndFlush(title);
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConflictException(exception);
        }
    }
}
