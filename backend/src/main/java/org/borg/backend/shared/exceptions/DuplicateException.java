package org.borg.backend.shared.exceptions;

import lombok.Builder;
import lombok.Getter;
import org.borg.backend.shared.enums.BusinessErrorCodes;

@Getter
@Builder
public class DuplicateException extends RuntimeException {

    private final BusinessErrorCodes errorCode;

    public DuplicateException(BusinessErrorCodes errorCode) {
        super(errorCode.getDescription());
        this.errorCode = errorCode;
    }
}
