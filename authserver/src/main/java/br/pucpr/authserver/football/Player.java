package br.pucpr.authserver.football;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.BatchSize;

@Entity(name = "FootballPlayer")
@Table(name = "FootballPlayer")
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    private int age;

    private int appearances;

    private int goals;

    private int assists;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal marketValue;

    @Column(nullable = false, length = 80)
    private String nationality;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teamId", nullable = false)
    private Team team;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "FootballPlayerTitle", joinColumns = @JoinColumn(name = "playerId"),
            inverseJoinColumns = @JoinColumn(name = "championshipId"))
    @BatchSize(size = 32)
    private Set<Championship> titles = new HashSet<>();

    protected Player() { }

    public Player(String name, int age, int appearances, int goals, int assists, BigDecimal marketValue, String nationality, Team team) {
        update(name, age, appearances, goals, assists, marketValue, nationality, team);
    }

    public void update(String name, int age, int appearances, int goals, int assists, BigDecimal marketValue, String nationality, Team team) {
        this.name = name.trim();
        this.age = age;
        this.appearances = appearances;
        this.goals = goals;
        this.assists = assists;
        this.marketValue = marketValue;
        this.nationality = nationality.trim();
        this.team = team;
    }

    public void transfer(Team team) { this.team = team; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public int getAppearances() { return appearances; }
    public int getGoals() { return goals; }
    public int getAssists() { return assists; }
    public BigDecimal getMarketValue() { return marketValue; }
    public String getNationality() { return nationality; }
    public Team getTeam() { return team; }
    public Set<Championship> getTitles() { return Set.copyOf(titles); }
    public void addTitle(Championship title) { titles.add(title); }
    public void removeTitle(Championship title) { titles.remove(title); }
}
