package org.borg.backend.player.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
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
    
    private int numOfGames;
    private int numOfWins;
    private int numOfLosses;
    private int numOfTies;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @MapKey(name = "category")
    private Map<String, CategoryStats> categoryStats = new HashMap<>();


    public void incrementWins() {
        this.numOfGames++;
        this.numOfWins++;
    }

    public void incrementLosses() {
        this.numOfGames++;
        this.numOfLosses++;
    }

    public void incrementTies() {
        this.numOfGames++;
        this.numOfTies++;
    }

    public void incrementQuestionsAnswered(String category) {
        categoryStats.computeIfAbsent(category, k -> new CategoryStats(category)).incrementQuestionsAnswered();
    }

    public void incrementCorrectAnswer(String category) {
        categoryStats.computeIfAbsent(category, k -> new CategoryStats(category)).incrementCorrectAnswers();
    }
}
