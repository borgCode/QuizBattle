package org.borg.backend.user;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("user")
@RequiredArgsConstructor
@Tag(name="User")
public class UserController {
    
    private final UserService userService;

    @GetMapping("/{username}")
    public UserDTO getUserById(@PathVariable String username) {
        return userService.getUserByName(username);
    }

    @PutMapping("/update")
    public ResponseEntity<Void> updateUser(@RequestBody UpdateUserRequest request) {
        userService.updateUser(request);
        return ResponseEntity.ok().build();  
    }
    
    //TODO add friend mapping
    
    //TODO send message mapping
}
   

