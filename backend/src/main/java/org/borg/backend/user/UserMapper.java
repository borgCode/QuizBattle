package org.borg.backend.user;

public class UserMapper {
    public static UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }
        
        return UserDTO.builder()
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .numOfGames(user.getNumOfGames())
                .numOfWins(user.getNumOfWins())
                .numOfLosses(user.getNumOfLosses())
                .build();
    }
}
