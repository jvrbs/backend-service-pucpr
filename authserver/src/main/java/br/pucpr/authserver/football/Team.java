package br.pucpr.authserver.football;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity(name = "FootballTeam")
@Table(name = "FootballTeam")
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private LocalDate foundedOn;

    @Column(nullable = false)
    private long supporters;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leagueId", nullable = false)
    private League league;

    protected Team() { }

    public Team(String name, LocalDate foundedOn, long supporters, League league) {
        update(name, foundedOn, supporters, league);
    }

    public void update(String name, LocalDate foundedOn, long supporters, League league) {
        this.name = name.trim();
        this.foundedOn = foundedOn;
        this.supporters = supporters;
        this.league = league;
    }

    public void join(League league) { this.league = league; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public LocalDate getFoundedOn() { return foundedOn; }
    public long getSupporters() { return supporters; }
    public League getLeague() { return league; }
}
