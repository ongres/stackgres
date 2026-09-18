package io.stackgres.matriarch.model;

import io.stackgres.matriarch.model.spec.RunIntent;

/**
 * A start/stop/delete was rejected because the cluster's desired run intent cannot make that move
 * (e.g. starting a cluster that is being deleted). The adapter maps it to a gRPC
 * {@code FAILED_PRECONDITION}.
 */
public class IllegalRunIntentTransitionException extends MatriarchException {

    public IllegalRunIntentTransitionException(String name, RunIntent from, RunIntent to) {
        super("The cluster " + name + " cannot go from " + from + " to " + to);
    }
}
