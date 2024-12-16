package org.borg.backend.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Set;
import java.util.stream.Collectors;
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ExceptionResponse> handleException(LockedException exception) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                        ExceptionResponse.builder()
                                .businessErrorCode(BusinessErrorCodes.ACCOUNT_LOCKED.getCode())
                                .businessErrorDescription(BusinessErrorCodes.ACCOUNT_LOCKED.getDescription())
                                .error(exception.getMessage())
                                .build()
                );
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ExceptionResponse> handleException(DisabledException exception) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                        ExceptionResponse.builder()
                                .businessErrorCode(BusinessErrorCodes.ACCOUNT_DISABLED.getCode())
                                .businessErrorDescription(BusinessErrorCodes.ACCOUNT_DISABLED.getDescription())
                                .error(exception.getMessage())
                                .build()
                );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ExceptionResponse> handleException(BadCredentialsException exception) {

        log.warn("Authentication failed: {}", exception.getMessage());
        return ResponseEntity
                .status(BusinessErrorCodes.BAD_CREDENTIALS.getHttpStatus())
                .body(
                        ExceptionResponse.builder()
                                .businessErrorCode(BusinessErrorCodes.BAD_CREDENTIALS.getCode())
                                .businessErrorDescription(BusinessErrorCodes.BAD_CREDENTIALS.getDescription())
                                .error(exception.getMessage())
                                .build()
                );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponse> handleException(MethodArgumentNotValidException exception) {
        Set<String> validationErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toSet());
        log.warn("Validation errors: " + validationErrors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ExceptionResponse.builder()
                        .validationErrors(validationErrors)
                        .build());
    }



    @ExceptionHandler(GameException.class)
    public ResponseEntity<ExceptionResponse> handleException(GameException exception) {
        ExceptionResponse response = ExceptionResponse.builder()
                .businessErrorCode(exception.getErrorCode().getCode())
                .businessErrorDescription(exception.getErrorCode().getDescription())
                .error(exception.getMessage())
                .build();
        
        return ResponseEntity
                .status(exception.getErrorCode().getHttpStatus())
                .body(response);
    }

    @ExceptionHandler(UserNameAlreadyTakenException.class)
    public ResponseEntity<ExceptionResponse> handleUsernameTaken(UserNameAlreadyTakenException exception) {
        return ResponseEntity.status(exception.getErrorCode().getHttpStatus())
                .body(ExceptionResponse.builder()
                        .businessErrorCode(exception.getErrorCode().getCode())
                        .businessErrorDescription(exception.getErrorCode().getDescription())
                        .error(exception.getMessage())
                        .build());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ExceptionResponse> handleMaxSizeException(MaxUploadSizeExceededException e) {
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(
                        ExceptionResponse.builder()
                                .businessErrorCode(BusinessErrorCodes.PROFILE_PIC_TOO_LARGE.getCode())
                                .businessErrorDescription(BusinessErrorCodes.PROFILE_PIC_TOO_LARGE.getDescription())
                                .error(e.getMessage())
                                .build()
                );
    }
    
    @ExceptionHandler(FriendshipException.class)
    public ResponseEntity<ExceptionResponse> handleException(FriendshipException exception) {
        ExceptionResponse response = ExceptionResponse.builder()
                .businessErrorCode(exception.getErrorCode().getCode())
                .businessErrorDescription(exception.getErrorCode().getDescription())
                .error(exception.getMessage())
                .build();
        
        return ResponseEntity
                .status(exception.getErrorCode().getHttpStatus())
                .body(response);
    }
    

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleException(Exception exception) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ExceptionResponse.builder()
                        .businessErrorDescription("Internal error, contact the admin")
                        .error(exception.getMessage())
                        .build());
    }
    
}
