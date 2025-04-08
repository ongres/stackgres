/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.runtime.ShutdownEvent;
import io.stackgres.common.OperatorProperty;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ReconciliatorWorkerThreadPool {

  protected static final Logger LOGGER = LoggerFactory.getLogger(
      ReconciliatorWorkerThreadPool.class.getName());

  private final PriorityBlockingQueue<Runnable> queue;

  private final PriorityBlockingQueue<Runnable> lowPriorityQueue;

  private final ReconciliatorThreadPoolExecutor executor;

  private final ReconciliatorThreadPoolExecutor lowPriorityExecutor;

  private final AtomicInteger threadIndex = new AtomicInteger(0);

  private final boolean enabled;

  private final Metrics metrics;

  private final long metricsScrapeMinInterval;

  private long lastMetricScrape = 0;

  @Inject
  public ReconciliatorWorkerThreadPool(Metrics metrics) {
    this.enabled = OperatorProperty.RECONCILIATION_ENABLE_THREAD_POOL
        .getBoolean();
    final Integer threads = OperatorProperty.RECONCILIATION_THREADS
        .get()
        .map(Integer::parseInt)
        .filter(n -> n > 0)
        .orElseGet(() -> (Runtime.getRuntime().availableProcessors() + 1) / 2);
    final Integer lowPriorityThreads = OperatorProperty.RECONCILIATION_LOW_PRIORITY_THREADS
        .get()
        .map(Integer::parseInt)
        .filter(n -> threads > n)
        .orElse(0);
    final boolean useFairness = !OperatorProperty.RECONCILIATION_DISABLE_FAIRNESS_QUEUE
        .getBoolean();
    final long fairnessWindow = OperatorProperty.RECONCILIATION_FAIRNESS_WINDOW
        .get()
        .map(Long::parseLong)
        .orElse(0L);
    if (this.enabled) {
      this.queue = new PriorityBlockingQueue<>();
      this.executor = new ReconciliatorThreadPoolExecutor(
          threads - lowPriorityThreads,
          useFairness,
          fairnessWindow,
          queue,
          r -> new Thread(r, "ReconciliationWorker-" + threadIndex.getAndIncrement()));
      if (lowPriorityThreads > 0) {
        this.lowPriorityQueue = new PriorityBlockingQueue<>();
        this.lowPriorityExecutor = new ReconciliatorThreadPoolExecutor(
            lowPriorityThreads,
            useFairness,
            fairnessWindow,
            lowPriorityQueue,
            r -> new Thread(r, "ReconciliationWorker-" + threadIndex.getAndIncrement()));
      } else {
        this.lowPriorityQueue = null;
        this.lowPriorityExecutor = null;
      }
    } else {
      this.queue = null;
      this.executor = null;
      this.lowPriorityQueue = null;
      this.lowPriorityExecutor = null;
    }
    this.metrics = metrics;
    this.metricsScrapeMinInterval = OperatorProperty.RECONCILIATION_THREAD_POOL_METRICS_SCRAPE_INTERVAL
        .get()
        .map(Long::parseLong)
        .filter(n -> n > 0L)
        .orElse(10000L);
  }

  void onStop(@Observes ShutdownEvent ev) {
    if (executor != null) {
      executor.shutdown();
    }
  }

  public void scheduleReconciliation(Runnable runnable, String configId, boolean priority) {
    if (!enabled) {
      runnable.run();
      return;
    }
    doScheduleReconciliation(runnable, configId, priority);
  }

  private synchronized void doScheduleReconciliation(Runnable runnable, String configId, boolean priority) {
    final long currentTimestamp = System.currentTimeMillis();
    final PriorityBlockingQueue<Runnable> queue;
    final ReconciliatorThreadPoolExecutor executor;
    if (lowPriorityExecutor != null && !priority && !this.queue.isEmpty()) {
      queue = this.lowPriorityQueue;
      executor = this.lowPriorityExecutor;
    } else {
      queue = this.queue;
      executor = this.executor;
    }
    var prioritizedRunnable = new ReconciliationRunnable(
        executor, runnable, configId, priority ? 1 : 0);
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("{} will be scheduled, current state of the pool:\n\nqueue:\n\n{}\n\nexecuting:\n\n{}\n",
          configId,
          getQueueTrace(queue),
          getExecutorTrace(executor, currentTimestamp));
    }
    var inversePrioritizedRunnable = new ReconciliationRunnable(
        executor, runnable, configId, !priority ? 1 : 0);
    if (priority) {
      final boolean removed = queue.remove(new ReconciliationRunnable(
          executor, runnable, configId, 0));
      if (removed) {
        LOGGER.trace("{} with low priority has been removed from the reconciliation queue", configId);
      }
    } else if (queue.contains(new ReconciliationRunnable(
        executor, runnable, configId, 1))) {
      LOGGER.trace("{} with high priority is already present in the reconciliation queue", configId);
      return;
    }
    if (executor.isExecuting(prioritizedRunnable)
        || executor.isExecuting(inversePrioritizedRunnable)) {
      LOGGER.trace("{} is already executing, will be scheduled to be reconcilied when current one finishes", configId);
      executor.executeWhenCompleted(prioritizedRunnable);
    } else {
      LOGGER.trace("{} has been scheduled to be reconcilied", configId);
      executor.execute(prioritizedRunnable);
    }

    if (currentTimestamp - lastMetricScrape > this.metricsScrapeMinInterval) {
      final Object[] queueArray = this.queue.toArray();
      metrics.gauge("reconciliation_pool_size",
          this.executor.threadPoolExecutor.getPoolSize());
      metrics.gauge("reconciliation_queue_size", queueArray.length);
      metrics.gauge("reconciliation_queue_size_high_priority",
          Stream.of(queueArray).map(ReconciliationRunnable.class::cast).filter(r -> r.priority % 2 != 0).count());
      metrics.gauge("reconciliation_queue_size_low_priority",
          Stream.of(queueArray).map(ReconciliationRunnable.class::cast).filter(r -> r.priority % 2 == 0).count());
      if (this.lowPriorityExecutor != null) {
        metrics.gauge("reconciliation_low_priority_pool_size",
            this.lowPriorityExecutor.threadPoolExecutor.getPoolSize());
        metrics.gauge("reconciliation_low_priority_queue_size",
            this.lowPriorityQueue.size());
      }
      lastMetricScrape = currentTimestamp;
    }
  }

  protected String getQueueTrace(
      PriorityBlockingQueue<Runnable> queue) {
    return Seq.of(queue.toArray())
        .map(ReconciliationRunnable.class::cast)
        .groupBy(r -> r.priority)
        .entrySet()
        .stream()
        .flatMap(group -> group.getValue().size() <= 10
            ? Seq.<Object>seq(group.getValue())
                : Seq.<Object>seq(group.getValue()).limit(10)
                .append("...and other " + (group.getKey() % 2 != 0 ? "high" : "low")
                    + " priority found: " + group.getValue().size()
                    + " (max " + group.getValue().stream()
                    .mapToLong(r -> r.timestamp)
                    .map(t -> System.currentTimeMillis() - t)
                    .max()
                    .orElse(0) + "ms)"))
        .map(Object::toString)
        .collect(Collectors.joining("\n"));
  }

  protected String getExecutorTrace(
      ReconciliatorThreadPoolExecutor executor,
      long currentTimestamp) {
    synchronized (executor.executingReconciliations) {
      return executor.executingReconciliations.entrySet().stream()
          .map(entry -> entry.getKey().toString() + " " + (currentTimestamp - entry.getValue()) + "ms")
          .collect(Collectors.joining("\n"));
    }
  }

  static class ReconciliationRunnable implements Runnable, Comparable<ReconciliationRunnable> {

    final ReconciliatorThreadPoolExecutor executor;
    final Runnable runnable;
    final long timestamp;
    final String configId;
    final int priority;
    final ClassLoader contextClassLoader;

    public ReconciliationRunnable(
        ReconciliatorThreadPoolExecutor executor,
        Runnable runnable,
        String configId,
        int priority) {
      this(executor, runnable, configId, priority, System.currentTimeMillis());
    }

    public ReconciliationRunnable(
        ReconciliatorThreadPoolExecutor executor,
        Runnable runnable,
        String configId,
        int priority,
        long timestamp) {
      this.executor = executor;
      this.runnable = runnable;
      this.timestamp = timestamp;
      this.configId = configId;
      this.priority = priority;
      this.contextClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @Override
    public void run() {
      executor.executeReconciliation(this);
    }

    @Override
    public int compareTo(ReconciliationRunnable o) {
      int compare = 0;
      if (executor.getUseFairness()) {
        compare = lastExecution().compareTo(o.lastExecution());
      }
      if (compare == 0) {
        compare = o.priority > priority ? 1 : (o.priority == priority ? 0 : -1);
      }
      if (compare == 0) {
        compare = o.timestamp > timestamp ? -1 : (o.timestamp == timestamp ? 0 : 1);
      }
      return compare;
    }

    private Long lastExecution() {
      Long lastExecution = executor.getLastExecution(configId);
      return lastExecution != null ? lastExecution : 0L;
    }

    @Override
    public int hashCode() {
      return Objects.hash(configId, priority);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof ReconciliationRunnable)) {
        return false;
      }
      ReconciliationRunnable other = (ReconciliationRunnable) obj;
      return Objects.equals(configId, other.configId)
          && Objects.equals(priority, other.priority);
    }

    @Override
    public String toString() {
      return (priority % 2 != 0 ? "* " : "  ") + configId + " " + (System.currentTimeMillis() - timestamp) + "ms";
    }

  }

  static class ReconciliatorThreadPoolExecutor {

    final ThreadPoolExecutor threadPoolExecutor;
    final Map<ReconciliationRunnable, Long> executingReconciliations = Collections.synchronizedMap(new HashMap<>());
    final Set<ReconciliationRunnable> toExecuteReconciliations = Collections.synchronizedSet(new HashSet<>());
    final Map<String, Long> lastExecutions = Collections.synchronizedMap(new HashMap<>());
    final boolean useFairness;
    final long fairnessWindow;

    public ReconciliatorThreadPoolExecutor(
        int threads,
        boolean useFairness,
        long fairnessWindow,
        BlockingQueue<Runnable> workQueue,
        ThreadFactory threadFactory) {
      this.threadPoolExecutor = new ThreadPoolExecutor(
          threads,
          threads,
          0L,
          TimeUnit.MILLISECONDS,
          workQueue,
          threadFactory);
      this.useFairness = useFairness;
      this.fairnessWindow = fairnessWindow;
    }

    public boolean getUseFairness() {
      return useFairness;
    }

    public Long getLastExecution(String configId) {
      return lastExecutions.get(configId);
    }

    void executeReconciliation(ReconciliationRunnable r) {
      executingReconciliations.put(r, System.currentTimeMillis());
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("{} started executing",
            r);
      }
      final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
      try {
        Thread.currentThread().setContextClassLoader(r.contextClassLoader);
        r.runnable.run();
      } finally {
        Thread.currentThread().setContextClassLoader(contextClassLoader);
        final long end = System.currentTimeMillis();
        lastExecutions.put(r.configId, fairnessWindow > 0 ? end / fairnessWindow : end);
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("{} finished executing after {}ms",
              r,
              Optional.ofNullable(executingReconciliations.get(r))
              .map(start -> end - start)
              .map(Object::toString)
              .orElse("?"));
        }
        executingReconciliations.remove(r);
        if (!isShutdown()
            && !isTerminated()) {
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("{} has been scheduled to be reconcilied",
                r);
          }
          toExecuteReconciliations.forEach(ter -> {
            if (ter.equals(r)) {
              this.execute(ter);
            }
          });
          toExecuteReconciliations.remove(r);
        }
      }
    }

    boolean isExecuting(ReconciliationRunnable r) {
      return executingReconciliations.containsKey(r);
    }

    void executeWhenCompleted(ReconciliationRunnable r) {
      toExecuteReconciliations.add(r);
    }

    public void execute(ReconciliationRunnable r) {
      threadPoolExecutor.execute(r);
    }

    public void shutdown() {
      this.threadPoolExecutor.shutdown();
    }

    public boolean isShutdown() {
      return this.threadPoolExecutor.isShutdown();
    }

    public boolean isTerminated() {
      return this.threadPoolExecutor.isTerminated();
    }

  }

}
