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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterControllerBootstrapImpl implements ClusterControllerBootstrap {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      ClusterControllerBootstrapImpl.class);

  private final KubernetesClient client;

  @Inject
  public ClusterControllerBootstrapImpl(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public void bootstrap() {
    try {
      if (client.getKubernetesVersion() != null) {
        LOGGER.info("Kubernetes version: {}", client.getKubernetesVersion().getGitVersion());
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
