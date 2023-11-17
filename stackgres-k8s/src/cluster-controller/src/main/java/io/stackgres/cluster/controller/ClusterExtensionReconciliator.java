/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.common.ClusterControllerEventReason;
import io.stackgres.cluster.common.StackGresClusterContext;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.extension.ExtensionEventEmitter;
import io.stackgres.common.extension.ExtensionManager;
import io.stackgres.common.extension.ExtensionReconciliator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class ClusterExtensionReconciliator
    extends ExtensionReconciliator<StackGresClusterContext> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterExtensionReconciliator.class);

  private final EventController eventController;

  @Dependent
  public static class Parameters {
    @Inject EventController eventController;
    @Inject ClusterControllerPropertyContext propertyContext;
    @Inject ExtensionManager extensionManager;
    @Inject ExtensionEventEmitter extensionEventEmitter;
  }

  @Inject
  public ClusterExtensionReconciliator(Parameters parameters) {
    super(parameters.propertyContext.getString(
        ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME),
        parameters.extensionManager,
        parameters.propertyContext.getBoolean(ClusterControllerProperty
            .CLUSTER_CONTROLLER_SKIP_OVERWRITE_SHARED_LIBRARIES),
        parameters.extensionEventEmitter);
    this.eventController = parameters.eventController;
  }

  @Override
  protected void onUninstallException(KubernetesClient client, StackGresCluster cluster,
      String extension, String podName, Exception ex) {
    String message = MessageFormatter.arrayFormat(
        "SGCluster {}.{}: uninstall of extension {} failed on pod {}",
        new String[] {
            cluster.getMetadata().getNamespace(),
            cluster.getMetadata().getName(),
            extension,
            podName
        }).getMessage();
    LOGGER.error(message, ex);
    try {
      eventController.sendEvent(ClusterControllerEventReason.CLUSTER_CONTROLLER_ERROR,
          message + ": " + ex.getMessage(), cluster, client);
    } catch (Exception rex) {
      LOGGER.error("Failed sending event while reconciling extension " + extension, rex);
    }
  }

  @Override
  protected void onInstallException(KubernetesClient client, StackGresCluster cluster,
      String extension, String podName, Exception ex) {
    String message = MessageFormatter.arrayFormat(
        "SGCluster {}.{}: install of extension {} failed on pod {}",
        new String[] {
            cluster.getMetadata().getNamespace(),
            cluster.getMetadata().getName(),
            extension,
            podName,
        }).getMessage();
    LOGGER.error(message, ex);
    try {
      eventController.sendEvent(ClusterControllerEventReason.CLUSTER_CONTROLLER_ERROR,
          message + ": " + ex.getMessage(), cluster, client);
    } catch (Exception rex) {
      LOGGER.error("Failed sending event while reconciling extension " + extension, rex);
    }
  }

}
