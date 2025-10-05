package com.github.maximvegorov.jdepends;

import java.util.List;

/**
 * Represents a provider of service definitions within the application.
 */
@FunctionalInterface
public interface Provider {
    List<ServiceDef> provides();
}
