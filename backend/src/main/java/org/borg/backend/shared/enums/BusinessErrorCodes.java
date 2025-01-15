package org.borg.backend.shared.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BusinessErrorCodes {

    INCORRECT_CURRENT_PASSWORD(100, HttpStatus.BAD_REQUEST, "Current password is incorrect"),
    NEW_PASSWORD_DOES_NOT_MATCH(101, HttpStatus.BAD_REQUEST, "New password does not match"),
    ACCOUNT_LOCKED(102, HttpStatus.FORBIDDEN, "User account is locked"),
    ACCOUNT_DISABLED(103, HttpStatus.FORBIDDEN, "User account is disabled"),
    BAD_CREDENTIALS(104, HttpStatus.FORBIDDEN, "Username and/or password is incorrect"),
    USERNAME_TAKEN(105, HttpStatus.BAD_REQUEST, "Username is taken"),
    
    
    FRIENDSHIP_REQUEST_PENDING(201, HttpStatus.BAD_REQUEST, "Friend request is already pending"),
    FRIENDSHIP_ALREADY_EXISTS(202, HttpStatus.BAD_REQUEST, "Friendship already exists"),
    FRIENDSHIP_NOT_FOUND(203, HttpStatus.BAD_REQUEST, "The friendship was not found"),
    CANNOT_FRIEND_SELF(206, HttpStatus.BAD_REQUEST, "Player cannot friend themselves"),
    
    ALREADY_BLOCKED(250, HttpStatus.BAD_REQUEST, "The player is already blocked"),
    CANNOT_BLOCK_SELF(251, HttpStatus.BAD_REQUEST, "Player can't block themselves"),
    CANNOT_UNBLOCK_SELF(252, HttpStatus.BAD_REQUEST, "Player can't unblock themselves"),
    CANNOT_UNBLOCK_WHEN_NOT_BLOCKED(253, HttpStatus.BAD_REQUEST, "Can't unblock player that is not blocked"),
    CANNOT_SEND_FRIEND_REQUEST_TO_BLOCKED(254,HttpStatus.BAD_REQUEST, "Player can't friend someone that they have blocked"),
    CANNOT_SEND_MATCH_REQUEST_TO_BLOCKED(255, HttpStatus.BAD_REQUEST, "Player can't send a match request to someone they blocked"),
    
    REMATCH_REQUEST_ALREADY_SENT(300, HttpStatus.BAD_REQUEST, "Rematch request already sent"),
    GAME_ALREADY_ONGOING(301, HttpStatus.BAD_REQUEST, "Players already have a game against each other ongoing"),
    NOT_PLAYER_TURN(302, HttpStatus.BAD_REQUEST, "It's not player's turn"),
    MUST_WAIT_FOR_OPPONENT(303, HttpStatus.BAD_REQUEST, "Player must wait for their opponent to finish their questions"),
    MUST_ANSWER_EXISTING_QUESTIONS(304, HttpStatus.BAD_REQUEST, "Player must answer the current questions before selecting a new category"),
    INVALID_QUESTION(305, HttpStatus.BAD_REQUEST, "The question does not belong to this session"),
    QUESTION_ALREADY_ANSWERED(306, HttpStatus.BAD_REQUEST, "Player has already answered this question"),
    NO_ACTIVE_SESSION(307 , HttpStatus.BAD_REQUEST, "No active sessions found for player"),
    INVALID_SESSION_STATE(308, HttpStatus.BAD_REQUEST, "Invalid session state"),
    NULL_REQUEST(309, HttpStatus.BAD_REQUEST, "Null request"),
    
    PROFILE_PIC_TOO_LARGE(400, HttpStatus.PAYLOAD_TOO_LARGE, "File size exceeds the maximum allowed limit of 500KB"),
    
    RESOURCE_NOT_FOUND(404, HttpStatus.NOT_FOUND, "Resource not found");

    private final int code;
    private final HttpStatus httpStatus;
    private final String description;


    BusinessErrorCodes(int code, HttpStatus httpStatus, String description) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.description = description;
    }
}
