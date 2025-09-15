package com.github.maximvegorov.jdepends;

import lombok.RequiredArgsConstructor;

public class Main {
    public static void main(String[] args) {
        var app = new App(
                Providers.supply(new ServiceA()),
                Providers.supply(new ServiceB()),
                Providers.provide(ServiceC.class,
                        (resolver, lifecycle) -> {
                            var result = new ServiceC(
                                    resolver.resolve(ServiceA.class),
                                    resolver.resolve(ServiceB.class));
                            lifecycle.registerStartStop(
                                    () -> System.out.println("Started"),
                                    () -> System.out.println("Stopped")
                            );
                            return result;
                        }));
        app.run(ServiceId.of(ServiceC.class));
    }

    public static final class ServiceA {
    }

    public static final class ServiceB {
    }

    @RequiredArgsConstructor
    public static final class ServiceC {
        private final ServiceA serviceA;
        private final ServiceB serviceB;
    }
}
