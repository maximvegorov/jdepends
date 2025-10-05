package com.github.maximvegorov.jdepends;

import lombok.NonNull;

/**
 * Represents the definition of a service within the application.
 *
 * @param serviceId the unique identifier of the service, not null
 * @param factory   the factory responsible for creating instances of the service, not null
 */
public record ServiceDef(@NonNull ServiceId serviceId, @NonNull Factory factory) {
}
