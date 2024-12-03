package org.borg.backend.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UpdateUserRequest {
    private Long userId;
    private UpdateField updateField;
    private String newUsername;
    private String newDisplayName;
}
