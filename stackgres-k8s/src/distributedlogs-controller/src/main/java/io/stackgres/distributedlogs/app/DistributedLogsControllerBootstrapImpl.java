/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.app;

import java.net.SocketTimeoutException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsDefinition;
import io.stackgres.distributedlogs.common.StackGresDistributedLogsContext;
import io.stackgres.operatorframework.resource.ResourceHandlerSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DistributedLogsControllerBootstrapImpl implements DistributedLogsControllerBootstrap {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      DistributedLogsControllerBootstrapImpl.class);

  private final KubernetesClientFactory clientFactory;
  private final ResourceHandlerSelector<StackGresDistributedLogsContext>
      distributedLogsHandlerSelector;

  @Inject
  public DistributedLogsControllerBootstrapImpl(
      KubernetesClientFactory clientFactory,
      ResourceHandlerSelector<StackGresDistributedLogsContext> distributedLogsHandlerSelector) {
    this.clientFactory = clientFactory;
    this.distributedLogsHandlerSelector = distributedLogsHandlerSelector;
  }

  @Override
  public void bootstrap() {

    try (KubernetesClient client = clientFactory.create()) {
      if (client.getVersion() != null) {
        LOGGER.info("Kubernetes version: {}", client.getVersion().getGitVersion());
      }
      LOGGER.info("URL of this Kubernetes cluster: {}", client.getMasterUrl());

      registerResources();
    } catch (KubernetesClientException e) {
      if (e.getCause() instanceof SocketTimeoutException) {
        LOGGER.error("Kubernetes cluster is not reachable, check your connection.");
      }
      throw e;
    }

  }

  private void registerResources() {
    KubernetesDeserializer.registerCustomKind(StackGresDistributedLogsDefinition.APIVERSION,
        StackGresDistributedLogsDefinition.KIND, StackGresDistributedLogs.class);

    distributedLogsHandlerSelector.registerKinds();
  }

}
