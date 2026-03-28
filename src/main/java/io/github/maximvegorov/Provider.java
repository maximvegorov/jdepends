package io.github.maximvegorov;

import java.util.List;

/**
 * Represents a provider of service definitions within the application.
 */
@FunctionalInterface
public interface Provider {
    List<ServiceDef> provides();
}
