package io.stackgres.matriarch.quarkus.event;

import io.stackgres.proto.slony.Event;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Bounded in-memory history of the slony agent's {@code Event} pushes (single local agent),
 * so {@code GetNodeEvents} can serve a snapshot. The agent's Event carries no timestamp, so
 * we stamp each one on arrival. Not durable — this matriarch session only.
 */
@ApplicationScoped
public class NodeEventStore {

    private static final int MAX = 500;

    /**
     * The received-at wall-clock millis paired with the agent's event.
     */
    public record Stored(long timestampMillis, Event event) {
    }

    private final Deque<Stored> events = new ArrayDeque<>();

    public synchronized void record(Event event) {
        events.addLast(new Stored(System.currentTimeMillis(), event));
        while (events.size() > MAX) {
            events.removeFirst();
        }
    }

    /**
     * Chronological snapshot of the agent's events.
     */
    public synchronized List<Stored> events() {
        return new ArrayList<>(events);
    }

}