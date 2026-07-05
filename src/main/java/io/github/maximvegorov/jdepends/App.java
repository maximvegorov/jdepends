package io.github.maximvegorov.jdepends;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The {@code App} class serves as the main entry point and orchestrator for an application built on a service-oriented
 * architecture. It manages the lifecycle of its components and the application state, providing mechanisms to
 * start and stop services properly.
 * This class utilizes a {@link Container} to manage and start the provided services and ensures proper cleanup
 * during application shutdown through the use of a shutdown hook.
 * It is designed to be used in a single-instance context and is thread-safe for managing its internal state.
 */
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
            log.info("Running app...");

            try {
                container.start(serviceIds);
            } catch (RuntimeException e) {
                log.error("Error while starting app", e);
                state.set(State.STOPPING);
                closeContainer(container);
                return ExitStatus.ERROR;
            }

            log.info("App run");

            var shutdownLatch = new CountDownLatch(1);

            var shutdownHook = new Thread(() -> {
                if (state.compareAndSet(State.STARTED, State.STOPPING)) {
                    try {
                        closeContainer(container);
                    } finally {
                        shutdownLatch.countDown();
                    }
                }
            }, "app-shutdown-hook");
            Runtime.getRuntime().addShutdownHook(shutdownHook);

            awaitUninterruptibly(shutdownLatch);

            log.info("App shutdown");

            return ExitStatus.SUCCESS;
        } finally {
            state.set(State.STOPPED);
        }
    }

    private static void closeContainer(Container container) {
        try {
            container.close();
        } catch (RuntimeException e) {
            log.warn("Error while closing container", e);
        }
    }

    private static void awaitUninterruptibly(CountDownLatch latch) {
        var interrupted = false;
        try {
            while (true) {
                try {
                    latch.await();
                    return;
                } catch (InterruptedException e) {
                    interrupted = true; // проглатываем и продолжаем ждать: выход — только через хук
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt(); // восстанавливаем флаг на выходе
            }
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
