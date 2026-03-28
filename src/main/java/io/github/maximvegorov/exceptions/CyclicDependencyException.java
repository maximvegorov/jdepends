package io.github.maximvegorov.exceptions;

public final class CyclicDependencyException extends RuntimeException {
    public CyclicDependencyException(String message) {
        super(message);
    }
}
