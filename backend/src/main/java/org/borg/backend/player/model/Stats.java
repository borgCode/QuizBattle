package org.borg.backend.player.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Stats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne
    @JoinColumn(name = "player_id")
    @JsonIgnore
    private Player player;
    private int numOfGames;
    private int numOfWins;
    private int numOfLosses;
    private int numOfTies;
    @OneToMany(mappedBy = "stats", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Map<String, CategoryStats> categoryStats;


    public void incrementWins() {
        this.numOfGames = this.numOfGames + 1;
        this.numOfWins = this.numOfWins + 1;
    }

    public void incrementLosses() {
        this.numOfGames = this.numOfGames + 1;
        this.numOfLosses = this.numOfLosses + 1;
    }
    
    public void incrementTies() {
        this.numOfGames = this.numOfGames + 1;
        this.numOfTies = this.numOfTies + 1;
    }
}
