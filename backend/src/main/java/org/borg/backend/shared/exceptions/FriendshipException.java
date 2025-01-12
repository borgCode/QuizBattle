package org.borg.backend.shared.exceptions;

import lombok.Builder;
import lombok.Getter;
import org.borg.backend.shared.enums.BusinessErrorCodes;

@Getter
@Builder
public class FriendshipException extends BaseException {
    
    public FriendshipException(BusinessErrorCodes errorCode, String message) {
        super(errorCode, message);
    }
}
