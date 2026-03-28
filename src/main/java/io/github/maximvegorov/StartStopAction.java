package io.github.maximvegorov;

/**
 * Encapsulates a pair of actions: a "start" action and a "stop" action.
 * Instances of this class are used to define behaviors that need to be executed
 * upon the start and stop of a particular service.
 */
record StartStopAction(Runnable startAction, Runnable stopAction) {
}
