package org.borg.backend.shared.exceptions;

import lombok.Getter;
import org.borg.backend.shared.enums.BusinessErrorCodes;

@Getter
public class GameException extends BaseException{
    
    public GameException(BusinessErrorCodes errorCode, String message) {
    super(errorCode, message);
    }
}
