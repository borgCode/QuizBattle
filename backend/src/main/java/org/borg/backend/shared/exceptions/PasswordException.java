package org.borg.backend.shared.exceptions;

import org.borg.backend.shared.enums.BusinessErrorCodes;

public class PasswordException extends BaseException {

  public PasswordException(BusinessErrorCodes errorCode, String message) {
    super(errorCode, message);
  }
}
