/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.kubernetesclient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class KubernetesClientProducer {

  private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesClientProducer.class);

  private volatile KubernetesClient client;
  private volatile KubernetesClient proxyClient;

  @Produces
  public KubernetesClient create() {
    LOGGER.trace("Returning proxy instance of Kubernetes Client");
    if (proxyClient == null) {
      createClient();
    }
    return proxyClient;
  }

  private synchronized void createClient() {
    if (proxyClient == null) {
      LOGGER.info("Creating proxy instance of Kubernetes client");
      client = new KubernetesClientBuilder().build();

      proxyClient = (KubernetesClient) Proxy
          .newProxyInstance(KubernetesClientProducer.class.getClassLoader(),
              new Class[] { KubernetesClient.class },
              new KubernetesClientInvocationHandler(client));
    }
  }

  @PreDestroy
  public void preDestroy() {
    LOGGER.info("Closing Kubernetes client");
    try {
      client.close();
    } catch (Exception ex) {
      LOGGER.warn("Can not close Kubernetes client", ex);
    }
  }

  private static class KubernetesClientInvocationHandler implements InvocationHandler {

    private final KubernetesClient client;

    public KubernetesClientInvocationHandler(KubernetesClient client) {
      this.client = client;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if ("close".equals(method.getName())) {
        LOGGER.trace("Ignoring close call of KubernetesClient instance.");
        return null;
      }
      try {
        return method.invoke(client, args);
      } catch (Exception ex) {
        if (ex.getCause() != null) {
          throw ex.getCause();
        } else {
          throw ex;
        }
      }
    }
  }

}
