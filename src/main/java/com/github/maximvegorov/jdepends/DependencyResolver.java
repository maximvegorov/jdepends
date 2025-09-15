package com.github.maximvegorov.jdepends;

@FunctionalInterface
public interface DependencyResolver {
    <T> T resolve(Class<T> klass, String name);

    default <T> T resolve(Class<T> klass) {
        return resolve(klass, null);
    }

    default Object resolve(ServiceId serviceId) {
        return resolve(serviceId.klass(), serviceId.name());
    }
}
