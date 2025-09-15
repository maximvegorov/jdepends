package com.github.maximvegorov.jdepends;

import com.github.maximvegorov.jdepends.exceptions.CyclicDependencyException;
import com.github.maximvegorov.jdepends.exceptions.FoundDuplicateException;
import com.github.maximvegorov.jdepends.exceptions.UnknownServiceIdException;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.BinaryOperator;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Slf4j
public final class Container implements AutoCloseable {
    private final Map<ServiceId, ServiceDef> registeredServices;
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
        this.resolvedServices = new HashMap<>();
        this.stopActions = new ArrayList<>();
    }

    public void start(ServiceId... serviceIds) {
        ensureNotClosed();

        for (var serviceId : serviceIds) {
            start(serviceId);
        }
    }

    public <T> T resolve(ServiceId serviceId) {
        ensureNotClosed();

        var result = resolvedServices.get(serviceId);
        if (result == null) {
            result = new ContainerDependencyResolver()
                    .resolve(serviceId);
        }

        @SuppressWarnings("unchecked")
        var castedResult = ((Class<T>) serviceId.klass()).cast(result);

        return castedResult;
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
                } catch (Exception e) {
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
                .resolve(serviceId.klass(), serviceId.name());

        log.info("Service started");
    }

    private final class ContainerDependencyResolver implements DependencyResolver {
        private final Set<ServiceId> resolvingServices = new HashSet<>();

        @Override
        public <T> T resolve(Class<T> klass, String name) {
            var serviceId = new ServiceId(klass, name);

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

                @SuppressWarnings("unchecked")
                var castedResult = ((Class<T>) serviceId.klass()).cast(result);

                return castedResult;
            } finally {
                resolvingServices.remove(serviceId);
            }
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
