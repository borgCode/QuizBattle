package org.borg.backend.handler;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserNameAlreadyTakenException extends RuntimeException {

    private final BusinessErrorCodes errorCode;

    public UserNameAlreadyTakenException(BusinessErrorCodes errorCode) {
        super(errorCode.getDescription());
        this.errorCode = errorCode;
    }
}
