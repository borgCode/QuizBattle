package org.borg.backend.singleplayer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.borg.backend.singleplayer.enums.ProgressStatus;

import java.time.LocalDate;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "stories")
public class Story {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;
    private String introText;
    private String base64Image;
    private Integer numOfChapters;
    private LocalDate startedAt;
    private LocalDate lastPlayed;
    private LocalDate completedAt;
    
    @Enumerated(EnumType.STRING)
    private ProgressStatus progressStatus;
}
