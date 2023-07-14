/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.kubernetesclient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ProxiedKubernetesClientProducer {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      ProxiedKubernetesClientProducer.class);

  private final KubernetesClient client;
  private final KubernetesClient proxyClient;

  @Inject
  public ProxiedKubernetesClientProducer(
      KubernetesSerialization kubernetesSerialization, Config config) {
    this.client = new KubernetesClientBuilder()
        .withKubernetesSerialization(kubernetesSerialization)
        .withConfig(config)
        .build();
    this.proxyClient = (KubernetesClient) Proxy
        .newProxyInstance(ProxiedKubernetesClientProducer.class.getClassLoader(),
            new Class[] { KubernetesClient.class },
            new KubernetesClientInvocationHandler(client));
  }

  @Produces
  public KubernetesClient create() {
    return proxyClient;
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

  public static class KubernetesClientInvocationHandler implements InvocationHandler {

    private final KubernetesClient client;

    public KubernetesClientInvocationHandler(KubernetesClient client) {
      this.client = client;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if ("close".equals(method.getName())) {
        LOGGER.warn("Ignoring close call of KubernetesClient instance.", new Exception());
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
