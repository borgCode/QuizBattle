package org.borg.backend.shared.exceptions;

import lombok.Builder;
import lombok.Getter;
import org.borg.backend.shared.enums.BusinessErrorCodes;

@Getter
@Builder
public class GameException extends BaseException{
    
    public GameException(BusinessErrorCodes errorCode, String message) {
    super(errorCode, message);
    }
}
