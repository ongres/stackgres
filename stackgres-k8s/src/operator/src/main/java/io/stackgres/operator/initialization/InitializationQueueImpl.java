/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.common.ConfigContext;
import io.stackgres.operator.common.ConfigProperty;
import io.stackgres.operator.resource.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InitializationQueueImpl implements InitializationQueue {

  public static final String LOCALHOST = "localhost";

  private static final Logger LOGGER = LoggerFactory
      .getLogger(InitializationQueueImpl.class);

  private static final String OPERATOR_HEALTH_URL_FORMAT = "http://%s:8080/health/ready";
  private static final String OPERATOR_SERVICE_FORMAT = "%s.%s.svc.cluster.local";

  private KubernetesClientFactory factory;

  private Queue<Runnable> initializationQueue = new ArrayDeque<>();

  private ScheduledExecutorService scheduler =
      Executors.newScheduledThreadPool(1, r -> new Thread(r, "InitializerQueueScheduler"));

  private final String operatorName;
  private final String operatorNamespace;
  private final String operatorIP;

  private InitializationStage stage = InitializationStage.STARTING;

  private enum InitializationStage {
    STARTING {
      @Override
      public InitializationStage nextState() {
        return OPERATOR_RESOLVABLE;
      }

    }, OPERATOR_RESOLVABLE {
      @Override
      public InitializationStage nextState() {
        return OPERATOR_SERVICE_RESOLVABLE;
      }
    }, OPERATOR_SERVICE_RESOLVABLE {
      @Override
      public InitializationStage nextState() {
        return POD_READY;
      }
    }, POD_READY {
      @Override
      public InitializationStage nextState() {
        return CONTAINERS_READY;
      }
    }, CONTAINERS_READY {
      @Override
      public InitializationStage nextState() {
        return null;
      }
    };

    public abstract InitializationStage nextState();
  }

  @Inject
  public InitializationQueueImpl(ConfigContext context, KubernetesClientFactory factory) {
    this.factory = factory;
    operatorName = context.getProperty(ConfigProperty.OPERATOR_NAME)
        .orElseThrow(() -> new IllegalStateException("Operator name is not configured"));
    operatorNamespace = context.getProperty(ConfigProperty.OPERATOR_NAMESPACE)
        .orElseThrow(() -> new IllegalStateException("Operator namespace is not configured"));
    operatorIP = context.getProperty(ConfigProperty.OPERATOR_IP)
        .orElseThrow(() -> new IllegalStateException("Operator ip is not configured"));
    if (operatorIP.equals(LOCALHOST)) {
      stage = InitializationStage.CONTAINERS_READY;
    }
  }

  boolean isOperatorServiceResolvable() throws UnknownHostException {

    String operatorServicePath = String
        .format(OPERATOR_SERVICE_FORMAT, operatorName, operatorNamespace);
    InetAddress
        .getByName(operatorServicePath);
    LOGGER.trace("Operator resolvable by service");
    return true;

  }

  boolean isOperatorResolvable() throws IOException {

    String operatorHealthUrl = String.format(OPERATOR_HEALTH_URL_FORMAT, operatorIP);

    HttpURLConnection con = null;
    try {

      URL url = new URL(operatorHealthUrl);
      con = (HttpURLConnection) url.openConnection();
      con.setRequestMethod("GET");

      con.setConnectTimeout(500);
      con.setReadTimeout(500);

      con.getResponseMessage();
      int status = con.getResponseCode();
      boolean operatorResolvable = status == HttpURLConnection.HTTP_OK;
      if (operatorResolvable) {
        LOGGER.trace("Operator resolvable by Ip address");
      } else {
        LOGGER.trace("Operator not resolvable by Ip address");
      }
      return operatorResolvable;

    } finally {
      if (con != null) {
        con.disconnect();
      }
    }
  }

  private <X> Optional<X> onRunningPod(Function<Pod, X> func) {
    try (KubernetesClient client = factory.create()) {
      Optional<Pod> pod = Optional.ofNullable(client.pods()
          .inNamespace(operatorNamespace)
          .withLabel(ResourceUtil.APP_KEY, operatorName)
          .list())
          .flatMap(podList -> {
            if (podList.getItems().isEmpty()) {
              return Optional.empty();
            } else {
              return Optional.of(podList.getItems().get(0));
            }
          });
      return pod.map(func);
    }
  }

  private boolean isPodReady() {

    Optional<Boolean> podReady = onRunningPod((pod) -> {
      String phase = pod.getStatus().getPhase();
      if (phase.equals("Running")) {
        LOGGER.trace("Operator's pod ready");
        return true;
      } else {
        LOGGER.trace("Operator pod is not ready, current phase: " + phase);
        return false;
      }
    });

    return podReady.orElseGet(() -> {
      LOGGER.trace("Operator pod not found");
      return false;
    });
  }

  private boolean areContainersReady() {

    Optional<Boolean> podReady = onRunningPod((pod) -> {
      long containersReady = pod.getStatus()
          .getContainerStatuses().stream()
          .filter(ContainerStatus::getReady).count();
      int declaredContainers = pod.getSpec().getContainers().size();
      boolean allContainersReady = declaredContainers == containersReady;
      if (allContainersReady) {
        LOGGER.trace("All operator's containers ready");
      } else {
        LOGGER.trace("Operator containers not ready");
      }
      return allContainersReady;
    });

    return podReady.orElseGet(() -> {
      LOGGER.trace("Operator pod not found");
      return false;
    });

  }

  private void flushQueue() {
    LOGGER.info("flushing initialization queue, tasks pending " + initializationQueue.size());

    int tasksPending = initializationQueue.size();

    for (int i = 0; i < tasksPending; i++) {
      Runnable task = initializationQueue.poll();
      try {
        task.run();
      } catch (Exception ex) {
        LOGGER.error("initialization task failed", ex);
        initializationQueue.add(task);
      }
    }

  }

  private boolean isOperatorReady() throws IOException {

    InitializationStage prevStage;
    do {
      prevStage = stage;
      switch (stage) {
        case STARTING:
          if (isOperatorResolvable()) {
            stage = stage.nextState();
          }
          break;
        case OPERATOR_RESOLVABLE:
          if (isOperatorServiceResolvable()) {
            stage = stage.nextState();
          }
          break;
        case OPERATOR_SERVICE_RESOLVABLE:
          if (isPodReady()) {
            stage = stage.nextState();
          }
          break;
        case POD_READY:
          if (areContainersReady()) {
            stage = stage.nextState();
          }
          break;
        case CONTAINERS_READY:
          prevStage = InitializationStage.CONTAINERS_READY;
          break;
        default:
          break;
      }

    } while (prevStage != stage && prevStage != InitializationStage.CONTAINERS_READY);
    return stage == InitializationStage.CONTAINERS_READY;
  }

  @PostConstruct
  void init() {
    LOGGER.trace("Checking if operator is ready");
    AtomicInteger retries = new AtomicInteger(0);
    scheduler.scheduleWithFixedDelay(() -> {
      boolean isOperatorReady = false;
      try {
        isOperatorReady = isOperatorReady();
      } catch (IOException e) {
        LOGGER.trace("operator not ready yet", e);
      }
      if (isOperatorReady) {
        int attempts = retries.addAndGet(1);
        flushQueue();
        if (initializationQueue.isEmpty() || attempts >= 5) {
          scheduler.shutdown();
        }
      }
    }, 500, 500, TimeUnit.MILLISECONDS);
  }

  @Override
  public void defer(Runnable r) {
    if (scheduler.isShutdown()) {
      r.run();
    } else {
      initializationQueue.offer(r);
    }
  }
}
