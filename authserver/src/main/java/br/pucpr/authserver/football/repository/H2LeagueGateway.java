package br.pucpr.authserver.football.repository;

import br.pucpr.authserver.football.gateway.LeagueGateway;
import br.pucpr.authserver.football.League;
import java.util.List;
import java.util.Optional;
import br.pucpr.authserver.exceptions.PersistenceConflictException;
import org.springframework.dao.DataIntegrityViolationException;

public class H2LeagueGateway implements LeagueGateway {
    private final LeagueRepository repository;

    public H2LeagueGateway(LeagueRepository repository) {
        this.repository = repository;
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public Optional<League> findById(long id) {
        return repository.findById(id);
    }

    @Override
    public List<League> findAllByOrderByNameAsc() {
        return repository.findAllByOrderByNameAsc();
    }

    @Override
    public League save(League league) {
        try {
            return repository.saveAndFlush(league);
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConflictException(exception);
        }
    }
}
