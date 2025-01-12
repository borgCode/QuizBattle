package org.borg.backend.shared.exceptions;

import lombok.Getter;
import org.borg.backend.shared.enums.BusinessErrorCodes;

@Getter
public abstract class BaseException extends RuntimeException {
    private final BusinessErrorCodes errorCode;

    protected BaseException(BusinessErrorCodes errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
