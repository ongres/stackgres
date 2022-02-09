/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.postgres;

import java.util.Objects;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.extension.ExtensionUtil;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PostgresBootstrapReconciliator {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(PostgresBootstrapReconciliator.class);

  private final ResourceFinder<Endpoints> endpointsFinder;
  private final String podName;

  public PostgresBootstrapReconciliator(
      ResourceFinder<Endpoints> endpointsFinder, String podName) {
    super();
    this.endpointsFinder = endpointsFinder;
    this.podName = podName;
  }

  public ReconciliationResult<Void> reconcile(KubernetesClient client, ClusterContext context)
      throws Exception {
    if (context.getCluster().getStatus() != null) {
      if (context.getCluster().getStatus().getArch() != null
          && context.getCluster().getStatus().getOs() != null) {
        if (!Objects.equals(
            context.getCluster().getStatus().getArch(),
            ExtensionUtil.OS_DETECTOR.getArch())
            || !Objects.equals(
                context.getCluster().getStatus().getOs(),
                ExtensionUtil.OS_DETECTOR.getOs())) {
          throw new IllegalStateException("The cluster was initialized with "
              + context.getCluster().getStatus().getArch()
              + "/" + context.getCluster().getStatus().getOs()
              + " but this instance is " + ExtensionUtil.OS_DETECTOR.getArch()
              + "/" + ExtensionUtil.OS_DETECTOR.getOs());
        }
        return new ReconciliationResult<>();
      }
    }
    try {
      Optional<Endpoints> patroniEndpoints = endpointsFinder
          .findByNameAndNamespace(PatroniUtil.name(context.getCluster()),
              context.getCluster().getMetadata().getNamespace());
      Optional<Endpoints> patroniConfigEndpoints = endpointsFinder
          .findByNameAndNamespace(PatroniUtil.configName(context.getCluster()),
              context.getCluster().getMetadata().getNamespace());
      if (patroniEndpoints.map(Endpoints::getMetadata)
          .map(ObjectMeta::getAnnotations)
          .map(annotations -> annotations.get(PatroniUtil.LEADER_KEY))
          .map(this.podName::equals).orElse(false)
          && patroniConfigEndpoints.map(Endpoints::getMetadata)
          .map(ObjectMeta::getAnnotations)
          .map(annotations -> annotations.get(PatroniUtil.INITIALIZE_KEY))
          .isPresent()) {
        if (context.getCluster().getStatus() == null) {
          context.getCluster().setStatus(new StackGresClusterStatus());
        }
        LOGGER.info("Cluster bootstrap completed");
        onClusterBootstrapped(client);
        context.getCluster().getStatus().setArch(ExtensionUtil.OS_DETECTOR.getArch());
        context.getCluster().getStatus().setOs(ExtensionUtil.OS_DETECTOR.getOs());
        LOGGER.info("Setting cluster arch {} and os {}",
            context.getCluster().getStatus().getArch(),
            context.getCluster().getStatus().getOs());
      }
    } catch (Exception ex) {
      return new ReconciliationResult<>(ex);
    }
    return new ReconciliationResult<>();
  }

  protected abstract void onClusterBootstrapped(KubernetesClient client);

}
