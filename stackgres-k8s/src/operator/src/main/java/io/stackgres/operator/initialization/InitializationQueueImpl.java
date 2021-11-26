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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.Application;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresPropertyContext;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InitializationQueueImpl implements InitializationQueue {

  private static final Logger LOGGER = LoggerFactory.getLogger(InitializationQueueImpl.class);

  private static final String OPERATOR_HEALTH_URL_FORMAT = "http://%s:8080/q/health/ready";
  private static final String OPERATOR_SERVICE_FORMAT = "%s.%s";
  private static final String LOCALHOST = "localhost";

  private final ScheduledExecutorService scheduler =
      Executors.newScheduledThreadPool(1, r -> new Thread(r, "InitializerQueueScheduler"));

  private final KubernetesClient client;
  private final String operatorName;
  private final String operatorNamespace;
  private final String operatorIP;
  private final List<Runnable> initializers;
  private final AtomicInteger retries = new AtomicInteger(0);

  private InitializationStage stage = InitializationStage.STARTING;

  @Inject
  public InitializationQueueImpl(KubernetesClient client,
                                 StackGresPropertyContext<OperatorProperty> context,
                                 @Any Instance<Initializer> initializers) {
    this.client = client;
    operatorName = context.get(OperatorProperty.OPERATOR_NAME)
        .orElseThrow(() -> new IllegalStateException("Operator name is not configured"));
    operatorNamespace = context.get(OperatorProperty.OPERATOR_NAMESPACE)
        .orElseThrow(() -> new IllegalStateException("Operator namespace is not configured"));
    operatorIP = context.get(OperatorProperty.OPERATOR_IP)
        .orElseThrow(() -> new IllegalStateException("Operator ip is not configured"));
    this.initializers = new ArrayList<>(Seq.seq(initializers).toList());
    if (operatorIP.equals(LOCALHOST)) {
      LOGGER.trace("Operator IP address is localhost");
      stage = InitializationStage.CONTAINERS_READY;
    }
  }

  @Override
  public void start() {
    LOGGER.trace("Checking if operator is ready");
    scheduler.schedule(this::initializationCycle, 500, TimeUnit.MILLISECONDS);

  }

  protected void initializationCycle() {
    boolean isOperatorReady = false;
    try {
      isOperatorReady = isOperatorReady();
    } catch (IOException e) {
      LOGGER.trace("Operator not ready yet", e);
    }

    if (isOperatorReady) {
      LOGGER.info("Flushing initialization queue, tasks pending " + initializers.size());
      int attempts = retries.addAndGet(1);
      final int size = initializers.size();
      for (int index = 0; index < size; index++) {
        Runnable initializer = initializers.remove(0);
        try {
          initializer.run();
        } catch (Exception ex) {
          LOGGER.warn("Initialization task failed", ex);
          initializers.add(initializer);
        }
      }
      if (initializers.isEmpty()) {
        scheduler.shutdown();
        return;
      }
      if (attempts >= 5 && !operatorIP.equals(LOCALHOST)) {
        LOGGER.error("Couldn't complete the initialization phase after 5 attemps.  "
            + "Shutting down...");
        new Thread(() -> Application.currentApplication().stop()).start();
        scheduler.shutdown();
        return;
      }
      scheduler.schedule(this::initializationCycle, 10, TimeUnit.SECONDS);
      return;
    }
    scheduler.schedule(this::initializationCycle, 500, TimeUnit.MILLISECONDS);
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
    Optional<Pod> pod = Optional.ofNullable(client.pods()
        .inNamespace(operatorNamespace)
        .withLabel(StackGresContext.APP_KEY, operatorName)
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

}
