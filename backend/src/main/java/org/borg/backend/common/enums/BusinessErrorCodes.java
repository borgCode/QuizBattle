package org.borg.backend.common.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BusinessErrorCodes {

    NO_CODE(0, HttpStatus.NOT_IMPLEMENTED, "No code"),
    INCORRECT_CURRENT_PASSWORD(300, HttpStatus.BAD_REQUEST, "Current password is incorrect"),
    NEW_PASSWORD_DOES_NOT_MATCH(301, HttpStatus.BAD_REQUEST, "New password does not match"),
    ACCOUNT_LOCKED(302, HttpStatus.FORBIDDEN, "User account is locked"),
    ACCOUNT_DISABLED(303, HttpStatus.FORBIDDEN, "User account is disabled"),
    BAD_CREDENTIALS(304, HttpStatus.FORBIDDEN, "Username and/or password is incorrect"),
    USERNAME_TAKEN(305, HttpStatus.BAD_REQUEST, "Username is taken"),
    FRIENDSHIP_ALREADY_BLOCKED(320, HttpStatus.BAD_REQUEST, "Cannot send friend request to blocked user"),
    FRIENDSHIP_REQUEST_PENDING(321, HttpStatus.BAD_REQUEST, "Friend request is already pending"),
    FRIENDSHIP_ALREADY_EXISTS(322, HttpStatus.BAD_REQUEST, "Friendship already exists"),
    FRIENDSHIP_NOT_FOUND(323, HttpStatus.BAD_REQUEST, "The friendship was not found"),
    REMATCH_REQUEST_ALREADY_SENT(324, HttpStatus.BAD_REQUEST, "Rematch request already sent"),
    GAME_ALREADY_ONGOING(325, HttpStatus.BAD_REQUEST, "Players already have a game against each other ongoing"),
    PROFILE_PIC_TOO_LARGE(413, HttpStatus.PAYLOAD_TOO_LARGE, "File size exceeds the maximum allowed limit of 500KB")
    ;

    private final int code;
    private final HttpStatus httpStatus;
    private final String description;


    BusinessErrorCodes(int code, HttpStatus httpStatus, String description) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.description = description;
    }
}
