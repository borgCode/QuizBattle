package org.borg.backend.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserDTO {
    private String username;
    private String displayName;
    private int numOfGames;
    private int numOfWins;
    private int numOfLosses;
}
