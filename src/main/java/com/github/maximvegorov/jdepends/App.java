package com.github.maximvegorov.jdepends;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public final class App {
    private final Container container;
    private final AtomicReference<State> state = new AtomicReference<>(State.NEW);

    public App(Provider... providers) {
        this(new Container(providers));
    }

    public App(@NonNull Container container) {
        this.container = container;
    }

    public ExitStatus run(ServiceId... serviceIds) {
        if (!state.compareAndSet(State.NEW, State.STARTED)) {
            throw new IllegalStateException("Invalid state: " + state.get());
        }
        try {
            var shutdownLock = new Object();

            var hook = new Thread(() -> {
                synchronized (shutdownLock) {
                    state.compareAndSet(State.STARTED, State.STOPPING);
                    shutdownLock.notifyAll();
                }
            }, "app-shutdown-hook");
            Runtime.getRuntime().addShutdownHook(hook);

            log.info("Running app...");
            try (container) {
                container.start(serviceIds);

                log.info("App run");

                synchronized (shutdownLock) {
                    while (state.get() == State.STARTED) {
                        shutdownLock.wait();
                    }
                }

                log.info("App shutdown");

                return ExitStatus.SUCCESS;
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                state.compareAndSet(State.STARTED, State.STOPPING);
                log.error("Error while running app", e);
                return ExitStatus.ERROR;
            } finally {
                try {
                    Runtime.getRuntime().removeShutdownHook(hook);
                } catch (IllegalStateException ignored) {
                }
            }
        } finally {
            state.set(State.STOPPED);
        }
    }

    public enum ExitStatus {
        SUCCESS,
        ERROR
    }

    private enum State {
        NEW,
        STARTED,
        STOPPING,
        STOPPED
    }
}
