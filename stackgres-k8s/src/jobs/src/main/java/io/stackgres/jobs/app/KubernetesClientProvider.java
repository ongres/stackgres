/*
 * Copyright (C) 2020 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.app;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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

  private static final MetricsHolder METRICS_HOLDER = new MetricsHolder();

  private final KubernetesClient targetKubernetesClient = new DefaultKubernetesClient();

  private final KubernetesClientInvocationHandler invocationHandler
      = new KubernetesClientInvocationHandler(targetKubernetesClient);

  private final KubernetesClient clientProxy = (KubernetesClient) Proxy.newProxyInstance(
      KubernetesClientProvider.class.getClassLoader(),
      new Class[]{KubernetesClient.class},
      invocationHandler
  );

  @Override
  public KubernetesClient create() {
    return clientProxy;
  }

  @PreDestroy
  public void preDestroy() {
    LOGGER.debug("Connections open on application stop {}", METRICS_HOLDER.openConnections.get());
    targetKubernetesClient.close();
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

  private static class KubernetesClientInvocationHandler implements InvocationHandler {

    private final KubernetesClient kubernetesClient;

    public KubernetesClientInvocationHandler(KubernetesClient kubernetesClient) {
      this.kubernetesClient = kubernetesClient;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

      if (method.getName().equals("close")) {
        return null;
      }

      if (LOGGER.isDebugEnabled()) {
        METRICS_HOLDER.newConnection();
      }
      try {
        return method.invoke(kubernetesClient, args);
      } catch (Exception ex) {
        if (ex.getCause() != null) {
          throw ex.getCause();
        } else {
          throw ex;
        }
      } finally {
        if (LOGGER.isDebugEnabled()) {
          METRICS_HOLDER.closeConnection();
        }
      }
    }
  }

}
