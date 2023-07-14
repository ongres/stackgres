/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.stackgres.cluster.common.ExtensionEventReason;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.event.ClusterEventEmitter;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ClusterFinder;
import io.stackgres.operatorframework.resource.EventReason;
import io.stackgres.testutil.StringUtils;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ExtensionEventEmitterImplTest {

  @InjectMock
  ClusterEventEmitter eventEmitter;

  @InjectMock
  ClusterFinder clusterFinder;

  @Inject
  ExtensionEventEmitterImpl extensionEventEmitter;

  @InjectMock
  ExtensionMetadataManager extensionManager;

  StackGresCluster cluster = Fixtures.cluster().loadDefault().get();

  String podName = ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME.getString();

  StackGresClusterInstalledExtension extension;

  @BeforeEach
  void setUp() {
    extension = new StackGresClusterInstalledExtension();
    extension.setBuild(StringUtils.getRandomString());
    extension.setName(StringUtils.getRandomString());
    extension.setVersion(StringUtils.getRandomString());
  }

  @Test
  @DisplayName("Given a extension download should emit an event")
  void emitExtensionDownloading() {

    withMockEventEmission(ExtensionEventReason.EXTENSION_DOWNLOADING,
        "Postgres extension " + extension.getName() + " is being downloaded into pod "
            + podName + ".",
        () -> extensionEventEmitter.emitExtensionDownloading(extension));

  }

  @Test
  @DisplayName("Given a extension deployed should emit an event")
  void emitExtensionDeployed() {

    withMockEventEmission(ExtensionEventReason.EXTENSION_DEPLOYED,
        "Postgres extension " + extension.getName() + " deployed to pod "
            + podName + ". Use CREATE EXTENSION in Postgres to load it.",
        () -> extensionEventEmitter.emitExtensionDeployed(extension));
  }

  @Test
  @DisplayName("Given a extension deployed that requires should emit an event")
  void emitExtensionDeployedRequiresRestart() {

    withMockEventEmission(ExtensionEventReason.EXTENSION_DEPLOYED_RESTART,
        "Postgres extension " + extension.getName() + " deployment requires a "
            + "cluster restart. To make it effective, use SgDbOps.[op==restart]. "
            + "Then run CREATE EXTENSION in Postgres to load it.",
        () -> extensionEventEmitter.emitExtensionDeployedRestart(extension));
  }

  @Test
  @DisplayName("Given an extension changed should emit an event")
  void emitExtensionChanged() {

    StackGresClusterInstalledExtension oldExtension = new StackGresClusterInstalledExtension();
    oldExtension.setName(extension.getName());
    oldExtension.setVersion(StringUtils.getRandomString());

    withMockEventEmission(ExtensionEventReason.EXTENSION_CHANGED,
        "Postgres extension " + extension.getName() + " changed from version "
            + oldExtension.getVersion() + " to " + extension.getVersion() + ". "
            + "Run ALTER EXTENSION $extension UPDATE in Postgres to update it.",
        () -> extensionEventEmitter.emitExtensionChanged(oldExtension, extension));
  }

  @Test
  @DisplayName("Given an extension removed should emit an event")
  void emitExtensionRemoved() {

    withMockEventEmission(ExtensionEventReason.EXTENSION_REMOVED,
        "Postgres extension " + extension.getName() + " removed from pod " + podName + ".",
        () -> extensionEventEmitter.emitExtensionRemoved(extension));

  }

  void withMockEventEmission(EventReason reason, String message, Runnable task) {
    when(clusterFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(cluster));

    doNothing().when(eventEmitter)
        .sendEvent(eq(reason), eq(message), eq(cluster));

    task.run();

    verify(clusterFinder).findByNameAndNamespace(any(), any());
    verify(eventEmitter).sendEvent(eq(reason), eq(message), eq(cluster));
  }
}
