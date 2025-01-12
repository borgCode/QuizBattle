package org.borg.backend.shared.exceptions;

import lombok.Getter;
import org.borg.backend.shared.enums.BusinessErrorCodes;

@Getter
public class ResourceNotFoundException extends BaseException {
    
    public ResourceNotFoundException(BusinessErrorCodes errorCode, String message) {
        super(errorCode, message);
    }
}
