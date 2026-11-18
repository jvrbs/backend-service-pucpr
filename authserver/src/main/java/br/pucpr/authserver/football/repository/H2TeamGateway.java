package br.pucpr.authserver.football.repository;

import br.pucpr.authserver.football.gateway.TeamGateway;
import br.pucpr.authserver.football.Team;
import java.util.List;
import java.util.Optional;
import br.pucpr.authserver.exceptions.PersistenceConflictException;
import org.springframework.dao.DataIntegrityViolationException;

public class H2TeamGateway implements TeamGateway {
    private final TeamRepository repository;

    public H2TeamGateway(TeamRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Team> findById(long id) {
        return repository.findById(id);
    }

    @Override
    public List<Team> findAllByOrderByNameAscIdAsc() {
        return repository.findAllByOrderByNameAscIdAsc();
    }

    @Override
    public List<Team> findByLeagueIdOrderByNameAscIdAsc(long leagueId) {
        return repository.findByLeagueIdOrderByNameAscIdAsc(leagueId);
    }

    @Override
    public Team save(Team team) {
        return repository.save(team);
    }

    @Override
    public void delete(Team team) {
        try {
            repository.delete(team);
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConflictException(exception);
        }
    }
}
