package org.borg.backend.shared.exceptions;

import lombok.Getter;
import org.borg.backend.shared.enums.BusinessErrorCodes;

@Getter
public class DuplicateException extends BaseException {
    
    public DuplicateException(BusinessErrorCodes errorCode, String message) {
        super(errorCode, message);
    }
}
