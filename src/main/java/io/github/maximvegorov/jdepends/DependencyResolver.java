package io.github.maximvegorov.jdepends;

import java.util.List;

/**
 * A interface that represents a mechanism for resolving dependencies.
 */
public interface DependencyResolver {
    Object resolveService(ServiceId serviceId);

    <T> List<T> resolveAll(Class<T> klass);

    default <T> T resolve(Class<T> klass) {
        return klass.cast(resolveService(ServiceId.of(klass)));
    }

   default  <T> T resolveNamed(Class<T> klass, String name) {
        return klass.cast(resolveService(new ServiceId(klass, name)));
    }
}
