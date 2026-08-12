package io.stackgres.cli.exec;

import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

/**
 * {@link SubmissionPublisher} that can store a single event for publication
 * once a subscriber subscribes.
 * Usually, {@link SubmissionPublisher}s only publish to current subscribers.
 * <br>
 * *NOTE:* The preloading only works for a single event and that that event is cleared after
 * the first subscription.
 */
public class PreloadedSubmissionPublisher<T> extends SubmissionPublisher<T> {

    private T initial;

    @Override
    public int submit(T item) {
        if (!hasSubscribers()) {
            initial = item;
            return 0;
        }
        return super.submit(item);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        super.subscribe(subscriber);
        if (initial != null) {
            submit(initial);
            initial = null;
        }
    }

}