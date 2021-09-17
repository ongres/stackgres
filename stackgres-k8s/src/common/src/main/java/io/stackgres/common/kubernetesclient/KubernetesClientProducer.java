/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.kubernetesclient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.annotation.PreDestroy;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.StackGresKubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class KubernetesClientProducer {

  private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesClientProducer.class);

  private final StackGresKubernetesClient targetKubernetesClient =
      new StackGresDefaultKubernetesClient();

  private final KubernetesClientInvocationHandler invocationHandler =
      new KubernetesClientInvocationHandler(targetKubernetesClient);

  private final StackGresKubernetesClient proxyClient = (StackGresKubernetesClient) Proxy
      .newProxyInstance(KubernetesClientProducer.class.getClassLoader(),
          new Class[] {StackGresKubernetesClient.class},
          invocationHandler);

  @Produces
  public KubernetesClient create() {
    LOGGER.debug("Returning proxy instance of StackGresKubernetesClient");
    return proxyClient;
  }

  @PreDestroy
  public void preDestroy() {
    LOGGER.info("Closing instance of StackGresKubernetesClient");
    targetKubernetesClient.close();
  }

  private static class KubernetesClientInvocationHandler implements InvocationHandler {

    private final StackGresKubernetesClient kubernetesClient;

    public KubernetesClientInvocationHandler(StackGresKubernetesClient kubernetesClient) {
      this.kubernetesClient = kubernetesClient;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if ("close".equals(method.getName())) {
        LOGGER.warn("Ignoring close call of KuberneteClient instance.");
        return null;
      }
      try {
        return method.invoke(kubernetesClient, args);
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
