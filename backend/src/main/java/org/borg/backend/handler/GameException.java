package org.borg.backend.handler;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GameException extends RuntimeException{
    private final BusinessErrorCodes errorCode;
    
    public GameException(BusinessErrorCodes errorCode) {
    super(errorCode.getDescription());
    this.errorCode = errorCode;
    }
}
