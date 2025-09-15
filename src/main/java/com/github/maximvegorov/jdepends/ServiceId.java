package com.github.maximvegorov.jdepends;

import lombok.NonNull;

public record ServiceId(@NonNull Class<?> klass, String name) {
    public static ServiceId of(Class<?> klass) {
        return new ServiceId(klass, null);
    }
}
