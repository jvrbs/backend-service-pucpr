package br.pucpr.authserver.football;

import jakarta.persistence.*;


@Entity(name = "FootballChampionship")
@Table(name = "FootballChampionship", uniqueConstraints = @UniqueConstraint(columnNames = {"leagueId", "season"}))
public class Championship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String season;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leagueId", nullable = false)
    private League league;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "championId", nullable = false)
    private Team champion;

    protected Championship() { }

    public Championship(String season, League league, Team champion) {
        this.season = season.trim();
        this.league = league;
        this.champion = champion;
    }

    public Long getId() { return id; }
    public String getSeason() { return season; }
    public League getLeague() { return league; }
    public Team getChampion() { return champion; }
}
