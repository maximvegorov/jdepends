package com.github.maximvegorov.jdepends;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public final class Providers {
    static <T> Provider supply(@NonNull T value) {
        var serviceId = ServiceId.of(value.getClass());
        var factory = (Factory) (resolver, lifecycle) -> value;
        return () -> List.of(new ServiceDef(serviceId, factory));
    }

    static <T> Provider supplyNamed(@NonNull String name, @NonNull T value) {
        var serviceId = new ServiceId(value.getClass(), name);
        var factory = (Factory) (resolver, lifecycle) -> value;
        return () -> List.of(new ServiceDef(serviceId, factory));
    }

    static <T> Provider provide(@NonNull Class<T> klass, @NonNull Factory factory) {
        var serviceId = ServiceId.of(klass);
        return () -> List.of(new ServiceDef(serviceId, factory));
    }

    static <T> Provider provideNamed(@NonNull String name, @NonNull Class<T> klass, @NonNull Factory factory) {
        var serviceId = new ServiceId(klass, name);
        return () -> List.of(new ServiceDef(serviceId, factory));
    }
}
