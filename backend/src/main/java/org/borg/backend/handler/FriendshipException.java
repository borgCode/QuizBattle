package org.borg.backend.handler;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FriendshipException extends RuntimeException {
    private final BusinessErrorCodes errorCode;

    public FriendshipException(BusinessErrorCodes errorCode) {
        super(errorCode.getDescription());
        this.errorCode = errorCode;
    }

}
