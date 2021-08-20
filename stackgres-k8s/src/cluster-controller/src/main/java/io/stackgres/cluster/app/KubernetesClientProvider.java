/*
 * Copyright (C) 2020 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.app;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.KubernetesClientFactory;

@ApplicationScoped
public class KubernetesClientProvider implements KubernetesClientFactory {

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
    targetKubernetesClient.close();
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
