package org.borg.backend.game.shared;


public record RoundAnswer(Long questionId, boolean correct, int index) {
}