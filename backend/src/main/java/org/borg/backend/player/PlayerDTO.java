package org.borg.backend.player;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PlayerDTO {
    private Long id;
    private String username;
    private String displayName;
    private int numOfGames;
    private int numOfWins;
    private int numOfLosses;
}
