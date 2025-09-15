package com.github.maximvegorov.jdepends.exceptions;

public final class CyclicDependencyException extends RuntimeException {
    public CyclicDependencyException(String message) {
        super(message);
    }
}
