/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops;

import java.lang.reflect.Proxy;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import io.smallrye.mutiny.vertx.MutinyHelper;
import io.stackgres.common.kubernetesclient.ProxiedKubernetesClientProducer.KubernetesClientInvocationHandler;
import io.vertx.core.Vertx;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Priority(1)
@Alternative
@Singleton
public class VertxKubernetesClientProducer {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      VertxKubernetesClientProducer.class);

  private final KubernetesClient client;
  private final KubernetesClient proxyClient;

  @Inject
  public VertxKubernetesClientProducer(
      KubernetesSerialization kubernetesSerialization, Config config, Vertx vertx) {
    this.client = new KubernetesClientBuilder()
        .withKubernetesSerialization(kubernetesSerialization)
        .withConfig(config)
        .withTaskExecutor(MutinyHelper.blockingExecutor(vertx))
        .build();
    this.proxyClient = (KubernetesClient) Proxy
        .newProxyInstance(VertxKubernetesClientProducer.class.getClassLoader(),
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

}
