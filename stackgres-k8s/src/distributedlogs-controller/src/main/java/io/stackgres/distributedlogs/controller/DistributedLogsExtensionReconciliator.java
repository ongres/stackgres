/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.controller;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.DistributedLogsControllerProperty;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.extension.ExtensionReconciliator;
import io.stackgres.distributedlogs.common.DistributedLogsControllerEventReason;
import io.stackgres.distributedlogs.common.StackGresDistributedLogsContext;
import io.stackgres.distributedlogs.configuration.DistributedLogsControllerPropertyContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class DistributedLogsExtensionReconciliator
    extends ExtensionReconciliator<StackGresDistributedLogsContext> {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      DistributedLogsExtensionReconciliator.class);

  private final EventController eventController;

  @Dependent
  public static class Parameters {
    @Inject EventController eventController;
    @Inject DistributedLogsControllerPropertyContext propertyContext;
    @Inject DistributedLogsExtensionManager extensionManager;
    @Inject ExtensionEventEmitterImpl extensionEventEmitter;
  }

  @Inject
  public DistributedLogsExtensionReconciliator(Parameters parameters) {
    super(parameters.propertyContext.getString(
        DistributedLogsControllerProperty.DISTRIBUTEDLOGS_CONTROLLER_POD_NAME),
        parameters.extensionManager,
        parameters.propertyContext.getBoolean(DistributedLogsControllerProperty
            .DISTRIBUTEDLOGS_CONTROLLER_SKIP_OVERWRITE_SHARED_LIBRARIES),
        parameters.extensionEventEmitter);
    this.eventController = parameters.eventController;
  }

  @Override
  protected void onUninstallException(KubernetesClient client, StackGresCluster cluster,
      String extension, String podName, Exception ex) {
    String message = MessageFormatter.arrayFormat(
        "StackGres Cluster {}.{}: uninstall of extension {} failed on pod {}",
        new String[] {
            cluster.getMetadata().getNamespace(),
            cluster.getMetadata().getName(),
            extension,
            podName
        }).getMessage();
    LOGGER.error(message, ex);
    try {
      eventController.sendEvent(
          DistributedLogsControllerEventReason.DISTRIBUTEDLOGS_CONTROLLER_ERROR,
          message + ": " + ex.getMessage(), cluster, client);
    } catch (Exception rex) {
      LOGGER.error("Failed sending event while reconciling extension " + extension, rex);
    }
  }

  @Override
  protected void onInstallException(KubernetesClient client, StackGresCluster cluster,
      String extension, String podName, Exception ex) {
    String message = MessageFormatter.arrayFormat(
        "StackGres Cluster {}.{}: install of extension {} failed on pod {}",
        new String[] {
            cluster.getMetadata().getNamespace(),
            cluster.getMetadata().getName(),
            extension,
            podName,
        }).getMessage();
    LOGGER.error(message, ex);
    try {
      eventController.sendEvent(
          DistributedLogsControllerEventReason.DISTRIBUTEDLOGS_CONTROLLER_ERROR,
          message + ": " + ex.getMessage(), cluster, client);
    } catch (Exception rex) {
      LOGGER.error("Failed sending event while reconciling extension " + extension, rex);
    }
  }

}
