package org.borg.backend.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;

    

    public User getUserByName(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void updateUser(UpdateUserRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        switch (request.getUpdateField()) {
            case USERNAME:
                user.setUsername(request.getNewUsername());
                break;
            case DISPLAY_NAME:
                user.setDisplayName(request.getNewDisplayName());
                break;
            default:
                throw new IllegalArgumentException("Invalid update field");
        }

        userRepository.save(user);
    }
}

