package org.borg.backend.common.exceptions;

import lombok.Builder;
import lombok.Getter;
import org.borg.backend.common.enums.BusinessErrorCodes;

@Getter
@Builder
public class UserNameAlreadyTakenException extends RuntimeException {

    private final BusinessErrorCodes errorCode;

    public UserNameAlreadyTakenException(BusinessErrorCodes errorCode) {
        super(errorCode.getDescription());
        this.errorCode = errorCode;
    }
}
