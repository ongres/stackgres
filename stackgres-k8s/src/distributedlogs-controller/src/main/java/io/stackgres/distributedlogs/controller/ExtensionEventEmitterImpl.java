/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.controller;

import java.util.function.Consumer;

import io.stackgres.common.DistributedLogsControllerProperty;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.extension.ExtensionEventEmitter;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.distributedlogs.common.ExtensionEventReason;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ExtensionEventEmitterImpl implements ExtensionEventEmitter {

  private final String podName = DistributedLogsControllerProperty
      .DISTRIBUTEDLOGS_CONTROLLER_POD_NAME.getString();

  private final String distributedLogsName = DistributedLogsControllerProperty
      .DISTRIBUTEDLOGS_NAME.getString();

  private final String namespace = DistributedLogsControllerProperty.DISTRIBUTEDLOGS_NAMESPACE
      .getString();

  private final CustomResourceFinder<StackGresDistributedLogs> distributedLogsFinder;

  private final EventEmitter<StackGresDistributedLogs> eventEmitter;

  @Inject
  public ExtensionEventEmitterImpl(
      CustomResourceFinder<StackGresDistributedLogs> distributedLogsFinder,
      EventEmitter<StackGresDistributedLogs> eventEmitter) {
    this.distributedLogsFinder = distributedLogsFinder;
    this.eventEmitter = eventEmitter;
  }

  @Override
  public void emitExtensionDownloading(StackGresClusterInstalledExtension extension) {
    withInvolvedObject(involvedObject ->
        eventEmitter.sendEvent(ExtensionEventReason.EXTENSION_DOWNLOADING,
            "Postgres extension " + extension.getName() + " is being downloaded into pod "
                + podName + ".",
            involvedObject)
    );
  }

  @Override
  public void emitExtensionDeployed(StackGresClusterInstalledExtension extension) {
    withInvolvedObject(involvedObject ->
        eventEmitter.sendEvent(ExtensionEventReason.EXTENSION_DEPLOYED,
            "Postgres extension " + extension.getName() + " deployed to pod "
                + podName + ".",
            involvedObject)
    );
  }

  @Override
  public void emitExtensionDeployedRestart(StackGresClusterInstalledExtension extension) {
    withInvolvedObject(involvedObject ->
        eventEmitter.sendEvent(ExtensionEventReason.EXTENSION_DEPLOYED_RESTART,
            "Postgres extension " + extension.getName() + " deployment requires a restart.",
            involvedObject)
    );
  }

  @Override
  public void emitExtensionChanged(StackGresClusterInstalledExtension oldExtension,
                                   StackGresClusterInstalledExtension newVersion) {
    withInvolvedObject(involvedObject ->
        eventEmitter.sendEvent(ExtensionEventReason.EXTENSION_CHANGED,
            "Postgres extension " + oldExtension.getName()
                + " changed from version " + oldExtension.getVersion() + " to "
                + newVersion.getVersion() + ".",
            involvedObject)
    );
  }

  @Override
  public void emitExtensionRemoved(StackGresClusterInstalledExtension extension) {
    withInvolvedObject(involvedObject ->
        eventEmitter.sendEvent(ExtensionEventReason.EXTENSION_REMOVED,
            "Postgres extension " + extension.getName() + " removed from pod "
                + podName + ".",
            involvedObject)
    );
  }

  private void withInvolvedObject(Consumer<StackGresDistributedLogs> emit) {
    StackGresDistributedLogs distributedLogs = distributedLogsFinder
        .findByNameAndNamespace(distributedLogsName, namespace)
        .orElseThrow();

    emit.accept(distributedLogs);
  }
}
