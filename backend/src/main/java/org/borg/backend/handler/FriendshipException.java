package org.borg.backend.handler;

public class FriendshipException extends RuntimeException {
    private final BusinessErrorCodes errorCode;

    public FriendshipException(BusinessErrorCodes errorCode) {
        super(errorCode.getDescription());
        this.errorCode = errorCode;
    }
    
    public BusinessErrorCodes getErrorCode() {
        return errorCode;
    }
}
