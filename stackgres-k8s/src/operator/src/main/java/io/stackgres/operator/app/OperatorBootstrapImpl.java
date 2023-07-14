/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import java.net.SocketTimeoutException;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class OperatorBootstrapImpl implements OperatorBootstrap {

  private static final Logger LOGGER = LoggerFactory.getLogger(OperatorBootstrapImpl.class);

  private final KubernetesClient client;

  @Inject
  public OperatorBootstrapImpl(
      KubernetesClient client) {
    this.client = client;
  }

  @Override
  public void bootstrap() {

    try {
      if (client.getKubernetesVersion() != null) {
        LOGGER.info("Kubernetes version: {}", client.getKubernetesVersion().getGitVersion());
      }
      LOGGER.info("URL of this Kubernetes cluster: {}", client.getMasterUrl());
      if (!hasCustomResource(client, StackGresCluster.class)
          || !hasCustomResource(client, StackGresProfile.class)
          || !hasCustomResource(client, StackGresPostgresConfig.class)
          || !hasCustomResource(client, StackGresPoolingConfig.class)
          || !hasCustomResource(client, StackGresBackupConfig.class)
          || !hasCustomResource(client, StackGresBackup.class)
          || !hasCustomResource(client, StackGresDistributedLogs.class)
          || !hasCustomResource(client, StackGresDbOps.class)
          || !hasCustomResource(client, StackGresShardedCluster.class)) {
        throw new RuntimeException("Some required CRDs does not exists");
      }

    } catch (KubernetesClientException e) {
      if (e.getCause() instanceof SocketTimeoutException) {
        LOGGER.error("Kubernetes cluster is not reachable, check your connection.");
      }
      throw e;
    }

  }

  private boolean hasCustomResource(KubernetesClient client,
      Class<? extends CustomResource<?, ?>> customResource) {
    final String crdName = CustomResource.getCRDName(customResource);
    if (!ResourceUtil.getCustomResource(client, crdName).isPresent()) {
      LOGGER.error("CRD not found, please create it first: {}", crdName);
      return false;
    }
    return true;
  }

}
