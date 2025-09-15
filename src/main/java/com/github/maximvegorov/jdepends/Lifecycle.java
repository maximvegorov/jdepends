package com.github.maximvegorov.jdepends;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public final class Lifecycle {
    @NonNull
    private final List<StartStopAction> actions;

    public void registerStartStop(@NonNull Runnable startAction, @NonNull Runnable stopAction) {
        actions.add(new StartStopAction(startAction, stopAction));
    }

    public void registerStart(@NonNull Runnable action) {
        actions.add(new StartStopAction(action, null));
    }

    public void registerStop(@NonNull Runnable action) {
        actions.add(new StartStopAction(null, action));
    }
}
