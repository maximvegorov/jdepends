package com.github.maximvegorov.jdepends;

/**
 * Represents a factory responsible for creating services based on the provided
 * {@link DependencyResolver} and {@link Lifecycle} instances.
 */
@FunctionalInterface
public interface Factory {
    Object create(DependencyResolver resolver, Lifecycle lifecycle);
}
