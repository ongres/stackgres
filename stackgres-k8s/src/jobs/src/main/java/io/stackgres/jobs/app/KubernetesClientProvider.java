/*
 * Copyright (C) 2020 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.app;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.KubernetesClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class KubernetesClientProvider implements KubernetesClientFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesClientProvider.class);

  private static final MetricsHolder metricsHolder = new MetricsHolder();

  @Override
  public KubernetesClient create() {
    metricsHolder.newConnection();
    DebugKubernetesClient client = new DebugKubernetesClient();
    client.setMetricsHolder(metricsHolder);
    return client;
  }

  @PreDestroy
  public void preDestroy() {
    LOGGER.debug("Connections open on application stop {}", metricsHolder.openConnections.get());
  }

  private static class MetricsHolder {
    AtomicInteger openConnections = new AtomicInteger(0);

    void newConnection() {
      int connections = openConnections.incrementAndGet();
      String callerName = Thread.currentThread().getStackTrace()[5].getClassName();
      LOGGER.debug("New connection requested by class {}, current open connections {}",
          callerName,
          connections);
    }

    void closeConnection() {
      int connections = openConnections.decrementAndGet();
      String callerName = Thread.currentThread().getStackTrace()[6].getClassName();
      LOGGER.debug("Connection closed by class {}, current open connections {}",
          callerName,
          connections);
    }
  }

  private static class DebugKubernetesClient extends DefaultKubernetesClient {

    private MetricsHolder metricsHolder;

    public void setMetricsHolder(MetricsHolder metricsHolder) {
      this.metricsHolder = metricsHolder;
    }

    @Override
    public void close() {
      metricsHolder.closeConnection();
      super.close();
    }
  }

}
