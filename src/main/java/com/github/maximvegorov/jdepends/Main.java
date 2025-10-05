package com.github.maximvegorov.jdepends;

import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        var app = new App(
                Providers.supply(new ServiceA()),
                Providers.supplyNamedAs("1", new NamedServiceB("1"), ServiceB.class),
                Providers.supplyNamedAs("2", new NamedServiceB("2"), ServiceB.class),
                Providers.provide(ServiceC.class,
                        (resolver, lifecycle) -> {
                            var servicesB = resolver.resolveAll(ServiceB.class);

                            var result = new ServiceC(
                                    resolver.resolve(ServiceA.class),
                                    servicesB);

                            lifecycle.registerStartStop(
                                    () -> System.out.println("Started"),
                                    () -> System.out.println("Stopped")
                            );
                            return result;
                        }));
        app.run(ServiceId.of(ServiceC.class));
    }

    public interface ServiceB {
    }

    public static final class ServiceA {
    }

    @RequiredArgsConstructor
    @ToString
    public static final class NamedServiceB implements ServiceB {
        private final String name;
    }

    @RequiredArgsConstructor
    public static final class ServiceC {
        private final ServiceA serviceA;
        private final List<ServiceB> servicesB;
    }
}
