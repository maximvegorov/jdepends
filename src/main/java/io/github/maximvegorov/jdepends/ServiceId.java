package io.github.maximvegorov.jdepends;

import lombok.NonNull;

/**
 * Represents a unique identifier for a service.
 */
public record ServiceId(@NonNull Class<?> klass, String name) {
    public static ServiceId of(Class<?> klass) {
        return new ServiceId(klass, null);
    }
}
