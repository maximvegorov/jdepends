package io.github.maximvegorov.jdepends;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.util.List;

/**
 * Utility class providing methods for creating {@link Provider} instances.
 */
@UtilityClass
public final class Providers {
    /**
     * Supplies a {@link Provider} that creates a predefined service instance.
     *
     * @param value the value to be provided as a service instance; must not be null
     * @param <T>   the type of the value to be supplied
     * @return a {@link Provider} that provides the service definition for the given value
     */
    public static <T> Provider supply(@NonNull T value) {
        var serviceId = ServiceId.of(value.getClass());
        var factory = (Factory) (resolver, lifecycle) -> value;
        return () -> List.of(new ServiceDef(serviceId, factory));
    }

    /**
     * Supplies a {@link Provider} that creates a predefined service instance with a specific type.
     *
     * @param <V>   the type of the value to be supplied
     * @param <I>   the type of the interface or class to which the value should conform
     * @param value the value to be provided as a service instance; must not be null
     * @param klass the class that represents the type of the service; must not be null
     * @return a {@link Provider} that provides the service definition for the given value and type
     * @throws IllegalArgumentException if the value is not compatible with the specified class
     */
    public static <V, I> Provider supplyAs(@NonNull V value, Class<I> klass) {
        if (!klass.isAssignableFrom(value.getClass())) {
            throw new IllegalArgumentException("%s incompatible with %s".formatted(value.getClass(), klass));
        }
        var serviceId = ServiceId.of(klass);
        var factory = (Factory) (resolver, lifecycle) -> value;
        return () -> List.of(new ServiceDef(serviceId, factory));
    }

    /**
     * Supplies a {@link Provider} that creates a predefined service instance with an assigned name.
     *
     * @param name  the name to associate with the service; must not be null
     * @param value the value to be provided as a service instance; must not be null
     * @param <T>   the type of the value to be supplied
     * @return a {@link Provider} that provides the service definition for the given value and name
     */
    public static <T> Provider supplyNamed(@NonNull String name, @NonNull T value) {
        var serviceId = new ServiceId(value.getClass(), name);
        var factory = (Factory) (resolver, lifecycle) -> value;
        return () -> List.of(new ServiceDef(serviceId, factory));
    }

    /**
     * Supplies a {@link Provider} that creates a service definition for a named service instance
     * with a specified type.
     *
     * @param <V>   the type of the value to be supplied
     * @param <T>   the type of the class to be associated with the value
     * @param name  the name to associate with the service; must not be null
     * @param value the value to be provided as a service instance; must not be null
     * @param klass the class that represents the type of the service; must not be null
     * @return a {@link Provider} that provides the service definition for the given name, value, and type
     * @throws IllegalArgumentException if the value is not compatible with the specified class
     */
    public static <V, T> Provider supplyNamedAs(@NonNull String name, @NonNull V value, Class<T> klass) {
        if (!klass.isAssignableFrom(value.getClass())) {
            throw new IllegalArgumentException("%s incompatible with %s".formatted(value.getClass(), klass));
        }
        var serviceId = new ServiceId(klass, name);
        var factory = (Factory) (resolver, lifecycle) -> value;
        return () -> List.of(new ServiceDef(serviceId, factory));
    }

    /**
     * Provides a {@link Provider} that defines a service for the given class and factory.
     *
     * @param klass   the class that represents the service type; must not be null
     * @param factory the factory responsible for creating instances of the service; must not be null
     * @param <T>     the type of the service to be provided
     * @return a {@link Provider} that supplies the service definition based on the class and factory
     */
    public static <T> Provider provide(@NonNull Class<T> klass, @NonNull Factory factory) {
        var serviceId = ServiceId.of(klass);
        return () -> List.of(new ServiceDef(serviceId, factory));
    }

    /**
     * Provides a {@link Provider} that defines a named service for the given class and factory.
     *
     * @param name    the name to associate with the service; must not be null
     * @param klass   the class that represents the service type; must not be null
     * @param factory the factory responsible for creating instances of the service; must not be null
     * @param <T>     the type of the service to be provided
     * @return a {@link Provider} that supplies the service definition based on the class, name, and factory
     */
    public static <T> Provider provideNamed(@NonNull String name, @NonNull Class<T> klass, @NonNull Factory factory) {
        var serviceId = new ServiceId(klass, name);
        return () -> List.of(new ServiceDef(serviceId, factory));
    }
}
