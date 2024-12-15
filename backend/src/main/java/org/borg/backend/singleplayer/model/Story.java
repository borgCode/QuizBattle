package org.borg.backend.singleplayer.model;

import jakarta.persistence.*;
import lombok.*;
import org.borg.backend.singleplayer.enums.ProgressStatus;

import java.time.LocalDate;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "stories")
public class Story {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;
    private String introText;
    private String imagePath;
    private Integer numOfChapters;
    
}
