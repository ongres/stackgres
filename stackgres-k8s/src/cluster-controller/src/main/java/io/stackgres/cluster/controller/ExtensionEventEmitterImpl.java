/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.util.function.Consumer;

import io.stackgres.cluster.common.ExtensionEventReason;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.extension.ExtensionEventEmitter;
import io.stackgres.common.resource.CustomResourceFinder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ExtensionEventEmitterImpl implements ExtensionEventEmitter {

  private final EventEmitter<StackGresCluster> clusterEventEmitter;

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  private final String podName = ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME.getString();

  private final String clusterName = ClusterControllerProperty.CLUSTER_NAME.getString();

  private final String namespace = ClusterControllerProperty.CLUSTER_NAMESPACE.getString();

  @Inject
  public ExtensionEventEmitterImpl(
      EventEmitter<StackGresCluster> clusterEventEmitter,
      CustomResourceFinder<StackGresCluster> clusterFinder) {
    this.clusterEventEmitter = clusterEventEmitter;
    this.clusterFinder = clusterFinder;
  }

  @Override
  public void emitExtensionDownloading(StackGresClusterInstalledExtension extension) {

    withInvolvedObject((involvedObject) ->
        clusterEventEmitter.sendEvent(ExtensionEventReason.EXTENSION_DOWNLOADING,
            "Postgres extension " + extension.getName()
                + " is being downloaded into pod " + podName + ".",
            involvedObject)
    );

  }

  @Override
  public void emitExtensionDeployed(StackGresClusterInstalledExtension extension) {
    withInvolvedObject((involvedObject) ->
        clusterEventEmitter.sendEvent(ExtensionEventReason.EXTENSION_DEPLOYED,
            "Postgres extension " + extension.getName()
                + " deployed to pod " + podName + ". "
                + "Use CREATE EXTENSION in Postgres to load it.",
            involvedObject)
    );
  }

  @Override
  public void emitExtensionDeployedRestart(StackGresClusterInstalledExtension extension) {
    withInvolvedObject((involvedObject) ->
        clusterEventEmitter.sendEvent(ExtensionEventReason.EXTENSION_DEPLOYED_RESTART,
            "Postgres extension " + extension.getName() + " deployment requires a cluster restart. "
                + "To make it effective, use SgDbOps.[op==restart]. "
                + "Then run CREATE EXTENSION in Postgres to load it.",
            involvedObject)
    );
  }

  @Override
  public void emitExtensionChanged(StackGresClusterInstalledExtension oldExtension,
                                   StackGresClusterInstalledExtension newVersion) {

    withInvolvedObject((involvedObject) ->
        clusterEventEmitter.sendEvent(ExtensionEventReason.EXTENSION_CHANGED,
            "Postgres extension " + oldExtension.getName() + " changed from version "
                + oldExtension.getVersion() + " to " + newVersion.getVersion() + ". "
                + "Run ALTER EXTENSION $extension UPDATE in Postgres to update it.",
            involvedObject)
    );

  }

  @Override
  public void emitExtensionRemoved(StackGresClusterInstalledExtension extension) {

    withInvolvedObject((involvedObject) ->
        clusterEventEmitter.sendEvent(ExtensionEventReason.EXTENSION_REMOVED,
            "Postgres extension " + extension.getName() + " removed from pod "
                + podName + ".",
            involvedObject)
    );

  }

  private void withInvolvedObject(Consumer<StackGresCluster> emit) {
    StackGresCluster cluster = clusterFinder.findByNameAndNamespace(clusterName, namespace)
        .orElseThrow();
    emit.accept(cluster);
  }
}
