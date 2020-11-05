/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.app;

import java.net.SocketTimeoutException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.stackgres.cluster.common.StackGresClusterContext;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.operatorframework.resource.ResourceHandlerSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterControllerBootstrapImpl implements ClusterControllerBootstrap {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      ClusterControllerBootstrapImpl.class);

  private final KubernetesClientFactory clientFactory;

  @Inject
  public ClusterControllerBootstrapImpl(
      KubernetesClientFactory clientFactory,
      ResourceHandlerSelector<StackGresClusterContext> clusterHandlerSelector) {
    this.clientFactory = clientFactory;
  }

  @Override
  public void bootstrap() {

    try (KubernetesClient client = clientFactory.create()) {
      if (client.getVersion() != null) {
        LOGGER.info("Kubernetes version: {}", client.getVersion().getGitVersion());
      }
      LOGGER.info("URL of this Kubernetes cluster: {}", client.getMasterUrl());
    } catch (KubernetesClientException e) {
      if (e.getCause() instanceof SocketTimeoutException) {
        LOGGER.error("Kubernetes cluster is not reachable, check your connection.");
      }
      throw e;
    }

  }

}
