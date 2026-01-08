package com.github.maximvegorov.jdepends;

import com.github.maximvegorov.jdepends.exceptions.CyclicDependencyException;
import com.github.maximvegorov.jdepends.exceptions.FoundDuplicateException;
import com.github.maximvegorov.jdepends.exceptions.UnknownServiceIdException;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.BinaryOperator;

import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;

/**
 * Represents a dependency injection container for managing the lifecycle of services.
 * The lifecycle management includes:
 * - Resolutions of services by their identifiers.
 * - Starting actions for initializing services.
 * - Stop actions for cleaning up resources in reverse order of initialization.
 * This class implements {@link AutoCloseable} to provide a structured way of handling resources.
 * It guarantees that services are disposed properly when the container is no longer needed.
 * Construction of this class requires at least one {@link Provider} that supplies the service definitions.
 * Error Handling:
 * - Throws {@link IllegalArgumentException} if no providers are supplied during instantiation.
 * - Detects cyclic dependencies between services and throws {@link CyclicDependencyException}.
 * - Throws {@link FoundDuplicateException} for duplicate service registrations.
 * - Throws {@link UnknownServiceIdException} when resolving a non-existent service.
 * - Attempting to use services after closure results in {@link IllegalStateException}.
 * Usage Guidelines:
 * - Always invoke {@link #close()} when the container is no longer needed to release resources.
 * - Ensure that all provided service configurations are unique and free from cyclic dependencies.
 * Construction Parameters:
 * - A varargs parameter of {@link Provider}, representing the service providers.
 *  Not thread-safe.
 */
@Slf4j
public final class Container implements AutoCloseable {
    private final Map<ServiceId, ServiceDef> registeredServices;
    private final Map<Class<?>, List<ServiceId>> registeredTypes;
    private final Map<ServiceId, Object> resolvedServices;
    private final List<Runnable> stopActions;
    private boolean closed;

    public Container(Provider... providers) {
        if (providers.length == 0) {
            throw new IllegalArgumentException("At least one provider must be provided");
        }

        var rejectDuplicateMerger = (BinaryOperator<ServiceDef>) (odlService, newService) -> {
            throw new FoundDuplicateException("Found duplicate for service %s".formatted(odlService.serviceId()));
        };

        this.registeredServices = Arrays.stream(providers)
                .flatMap(provider -> provider.provides().stream())
                .collect(toMap(ServiceDef::serviceId, identity(), rejectDuplicateMerger));
        this.registeredTypes = this.registeredServices.keySet()
                .stream()
                .collect(groupingBy(ServiceId::klass, toList()));
        this.resolvedServices = new HashMap<>();
        this.stopActions = new ArrayList<>();
    }

    public void start(ServiceId... serviceIds) {
        ensureNotClosed();

        for (var serviceId : serviceIds) {
            start(serviceId);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        log.info("Closing container...");
        try {
            for (var i = stopActions.size() - 1; i >= 0; i--) {
                var stopAction = stopActions.get(i);
                try {
                    stopAction.run();
                } catch (RuntimeException e) {
                    log.error("Unexpected error while calling stop action with index {}", i, e);
                }
            }

            stopActions.clear();
            resolvedServices.clear();
            registeredServices.clear();

            log.info("Container closed");
        } finally {
            closed = true;
        }
    }

    private void ensureNotClosed() {
        if (closed) {
            throw new IllegalStateException("Container is closed");
        }
    }

    private void start(ServiceId serviceId) {
        if (resolvedServices.containsKey(serviceId)) {
            return;
        }

        log.info("Starting service {}...", serviceId);

        new ContainerDependencyResolver()
                .resolveService(new ServiceId(serviceId.klass(), serviceId.name()));

        log.info("Service started");
    }

    private final class ContainerDependencyResolver implements DependencyResolver {
        private final Set<ServiceId> resolvingServices = new HashSet<>();

        @Override
        public Object resolveService(ServiceId serviceId) {
            log.info("Resolving service {}...", serviceId);

            if (resolvingServices.contains(serviceId)) {
                throw new CyclicDependencyException("Cyclic dependency detected for service %s".formatted(serviceId));
            }

            resolvingServices.add(serviceId);
            try {
                var result = resolvedServices.get(serviceId);
                if (result == null) {
                    var serviceDef = registeredServices.get(serviceId);
                    if (serviceDef == null) {
                        throw new UnknownServiceIdException("No service was registered for %s".formatted(serviceId));
                    }

                    result = createService(serviceDef);

                    resolvedServices.put(serviceId, result);
                }

                log.info("Resolved {} to {}", serviceId, result);

                return serviceId.klass().cast(result);
            } finally {
                resolvingServices.remove(serviceId);
            }
        }

        @Override
        public <T> List<T> resolveAll(Class<T> klass) {
            return registeredTypes.getOrDefault(klass, emptyList())
                    .stream()
                    .map(this::resolveService)
                    .map(klass::cast)
                    .toList();
        }

        private Object createService(ServiceDef serviceDef) {
            var performedActions = new ArrayList<Runnable>();

            var startStopActions = new ArrayList<StartStopAction>();

            var result = serviceDef.factory().create(this, new Lifecycle(startStopActions));
            try {
                for (var startStopAction : startStopActions) {
                    if (startStopAction.startAction() != null) {
                        startStopAction.startAction().run();
                    }
                    if (startStopAction.stopAction() != null) {
                        performedActions.add(startStopAction.stopAction());
                    }
                }
                stopActions.addAll(performedActions);
            } catch (Exception error) {
                for (var i = performedActions.size() - 1; i >= 0; i--) {
                    var stopAction = performedActions.get(i);
                    try {
                        stopAction.run();
                    } catch (Exception e) {
                        error.addSuppressed(e);
                    }
                }
                throw error;
            }
            return result;
        }
    }
}
