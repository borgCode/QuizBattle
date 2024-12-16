package org.borg.backend.common.exceptions;

import lombok.Builder;
import lombok.Getter;
import org.borg.backend.common.enums.BusinessErrorCodes;

@Getter
@Builder
public class FriendshipException extends RuntimeException {
    private final BusinessErrorCodes errorCode;

    public FriendshipException(BusinessErrorCodes errorCode) {
        super(errorCode.getDescription());
        this.errorCode = errorCode;
    }

}
