package br.pucpr.authserver.football;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity(name = "FootballLeague")
@Table(name = "FootballLeague")
public class League {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(nullable = false, length = 80)
    private String country;

    @Column(nullable = false)
    private LocalDate foundedOn;

    protected League() { }

    public League(String name, String country, LocalDate foundedOn) {
        this.name = name.trim();
        this.country = country.trim();
        this.foundedOn = foundedOn;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCountry() { return country; }
    public LocalDate getFoundedOn() { return foundedOn; }
}
