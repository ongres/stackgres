/*
 * Copyright (C) 2020 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.StackGresDefaultKubernetesClient;
import io.stackgres.common.StackGresKubernetesClient;
import io.stackgres.common.StackGresKubernetesClientFactory;

@ApplicationScoped
public class KubernetesClientProvider implements StackGresKubernetesClientFactory {

  private final StackGresKubernetesClient targetKubernetesClient
      = new StackGresDefaultKubernetesClient();

  private final KubernetesClientInvocationHandler invocationHandler
      = new KubernetesClientInvocationHandler(targetKubernetesClient);

  private final StackGresKubernetesClient proxyClient = (StackGresKubernetesClient) Proxy
      .newProxyInstance(
          KubernetesClientProvider.class.getClassLoader(),
          new Class[]{StackGresKubernetesClient.class},
          invocationHandler
      );

  @Override
  public StackGresKubernetesClient create() {
    return proxyClient;
  }

  @PreDestroy
  public void preDestroy() {
    targetKubernetesClient.close();
  }

  private static class KubernetesClientInvocationHandler implements InvocationHandler {

    private final StackGresKubernetesClient kubernetesClient;

    public KubernetesClientInvocationHandler(StackGresKubernetesClient kubernetesClient) {
      this.kubernetesClient = kubernetesClient;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

      if (method.getName().equals("close")) {
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
