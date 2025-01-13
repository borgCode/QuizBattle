package org.borg.backend.shared.exceptions;

import org.borg.backend.shared.enums.BusinessErrorCodes;

public class BlockException extends BaseException {

    public BlockException(BusinessErrorCodes errorCode, String message) {
        super(errorCode, message);
    }
}
