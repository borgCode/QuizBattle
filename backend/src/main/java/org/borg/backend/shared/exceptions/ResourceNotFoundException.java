package org.borg.backend.shared.exceptions;

import lombok.Getter;
import org.borg.backend.shared.enums.BusinessErrorCodes;

@Getter
public class ResourceNotFoundException extends RuntimeException {
    private final BusinessErrorCodes errorCode;
    public ResourceNotFoundException( BusinessErrorCodes errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
