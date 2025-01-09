package org.borg.backend.game.shared.model;


public record RoundAnswer(Long questionId, boolean correct, int index) {
}