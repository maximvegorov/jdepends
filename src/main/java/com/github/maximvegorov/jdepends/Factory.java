package com.github.maximvegorov.jdepends;

@FunctionalInterface
public interface Factory {
    Object create(DependencyResolver resolver, Lifecycle lifecycle);
}
