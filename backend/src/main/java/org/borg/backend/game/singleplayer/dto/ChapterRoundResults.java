package org.borg.backend.game.singleplayer.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ChapterRoundResults {
    private List<Boolean> questionResults;
    private boolean isRoundPassed;
    private boolean isGameOver;
    private boolean isChapterComplete;
    private int currentHealth;
}
