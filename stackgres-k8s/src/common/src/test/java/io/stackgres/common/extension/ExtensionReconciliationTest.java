/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.extension.ExtensionManager.ExtensionInstaller;
import io.stackgres.common.extension.ExtensionManager.ExtensionUninstaller;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class ExtensionReconciliationTest {

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.getOrderedVersions().findFirst().get();

  private static final String POSTGRES_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getOrderedMajorVersions().findFirst().get();

  private static final String BUILD_VERSION =
      StackGresComponent.POSTGRESQL.getOrderedBuildVersions().findFirst().get();

  @Mock
  private ExtensionManager extensionManager;

  @Mock
  private ExtensionInstaller extensionInstaller;

  @Mock
  private ExtensionUninstaller extensionUninstaller;

  @Mock
  private ExtensionReconciliatorContext context;

  private ExtensionReconciliator<ExtensionReconciliatorContext> initReconciliator;

  private ExtensionReconciliator<ExtensionReconciliatorContext> reconciliator;

  @BeforeEach
  void setUp() throws Exception {
    initReconciliator = new ExtensionReconciliator<ExtensionReconciliatorContext>("test-0",
        extensionManager, false) {
      @Override
      protected void onUninstallException(KubernetesClient client, StackGresCluster cluster,
          String extension, String podName, Exception ex) {
        throw new RuntimeException(ex);
      }

      @Override
      protected void onInstallException(KubernetesClient client, StackGresCluster cluster,
          String extension, String podName, Exception ex) {
        throw new RuntimeException(ex);
      }
    };
    reconciliator = new ExtensionReconciliator<ExtensionReconciliatorContext>("test-0",
        extensionManager, true) {
      @Override
      protected void onUninstallException(KubernetesClient client, StackGresCluster cluster,
          String extension, String podName, Exception ex) {
        throw new RuntimeException(ex);
      }

      @Override
      protected void onInstallException(KubernetesClient client, StackGresCluster cluster,
          String extension, String podName, Exception ex) {
        throw new RuntimeException(ex);
      }
    };
  }

  private StackGresClusterExtension getExtension() {
    StackGresClusterExtension extension = new StackGresClusterExtension();
    extension.setName("timescaledb");
    return extension;
  }

  private StackGresClusterInstalledExtension getInstalledExtension() {
    StackGresClusterInstalledExtension installedExtension =
        new StackGresClusterInstalledExtension();
    installedExtension.setName("timescaledb");
    installedExtension.setPublisher("com.ongres");
    installedExtension.setRepository(null);
    installedExtension.setVersion("1.7.1");
    installedExtension.setPostgresVersion(POSTGRES_MAJOR_VERSION);
    installedExtension.setBuild(BUILD_VERSION);
    return installedExtension;
  }

  private ExtensionReconciliatorContext getContext(Consumer<StackGresCluster> consumer) {
    StackGresCluster cluster = JsonUtil
        .readFromJson("stackgres_cluster/list.json",
            StackGresClusterList.class)
        .getItems().get(0);
    cluster.getSpec().setPostgresVersion(POSTGRES_VERSION);
    consumer.accept(cluster);
    when(context.getCluster()).thenReturn(cluster);
    when(context.getExtensions()).thenReturn(
        Optional.ofNullable(cluster.getSpec().getPostgresExtensions())
            .map(ImmutableList::copyOf)
            .orElse(ImmutableList.of()));
    return context;
  }

  @Test
  void testReconciliationWithExtension_installIsPerformed() throws Exception {
    StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    StackGresClusterExtension extension = getExtension();
    when(extensionManager.getExtensionInstaller(any(), any(StackGresClusterExtension.class)))
        .thenReturn(extensionInstaller);
    when(extensionInstaller
        .isExtensionInstalled())
            .thenReturn(false);
    when(extensionInstaller
        .isExtensionPendingOverwrite())
            .thenReturn(false);
    when(extensionInstaller
        .getInstalledExtension())
            .thenReturn(installedExtension);
    ExtensionReconciliatorContext context = getContext(cluster -> {
      cluster.getSpec().setPostgresExtensions(new ArrayList<>());
      cluster.getSpec().getPostgresExtensions().add(extension);
    });
    Assertions.assertTrue(reconciliator.reconcile(null, context).result().get());
    Assertions.assertTrue(Optional.of(context.getCluster())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getPodStatuses)
        .stream().flatMap(List::stream)
        .anyMatch(podStatus -> podStatus.getName().equals("test-0")));
    Assertions.assertFalse(context.getCluster().getStatus().getPodStatuses()
        .stream().filter(podStatus -> podStatus.getName().equals("test-0"))
        .findAny().map(StackGresClusterPodStatus::getPendingRestart).orElse(false));
    Assertions.assertIterableEquals(ImmutableList.of(installedExtension),
        context.getCluster().getStatus().getPodStatuses()
            .stream().filter(podStatus -> podStatus.getName().equals("test-0"))
            .findAny().map(StackGresClusterPodStatus::getInstalledPostgresExtensions)
            .stream().flatMap(List::stream).collect(ImmutableList.toImmutableList()));
    verify(extensionInstaller, times(1)).isExtensionInstalled();
    verify(extensionInstaller, times(0)).isLinksCreated();
    verify(extensionUninstaller, times(0)).isExtensionInstalled();
    verify(extensionInstaller, times(1)).getInstalledExtension();
    verify(extensionInstaller, times(1)).downloadAndExtract();
    verify(extensionInstaller, times(1)).verify();
    verify(extensionInstaller, times(1)).installExtension();
    verify(extensionInstaller, times(0)).createExtensionLinks();
    verify(extensionInstaller, times(1)).doesInstallOverwriteAnySharedLibrary();
    verify(extensionInstaller, times(0)).setExtensionAsPending();
    verify(extensionUninstaller, times(0)).uninstallExtension();
  }

  @Test
  void testReconciliationWithExtensionAlreadyPresent_installIsSkippedButStatusUpdated()
      throws Exception {
    StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    StackGresClusterExtension extension = new StackGresClusterExtension();
    when(extensionManager.getExtensionInstaller(any(), any(StackGresClusterExtension.class)))
        .thenReturn(extensionInstaller);
    when(extensionInstaller
        .isExtensionInstalled())
            .thenReturn(true);
    when(extensionInstaller
        .isLinksCreated())
            .thenReturn(true);
    when(extensionInstaller
        .getInstalledExtension())
            .thenReturn(installedExtension);
    ExtensionReconciliatorContext context = getContext(cluster -> {
      extension.setName("timescaledb");
      cluster.getSpec().setPostgresExtensions(new ArrayList<>());
      cluster.getSpec().getPostgresExtensions().add(extension);
    });
    Assertions.assertTrue(reconciliator.reconcile(null, context).result().get());
    Assertions.assertTrue(Optional.of(context.getCluster())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getPodStatuses)
        .stream().flatMap(List::stream)
        .anyMatch(podStatus -> podStatus.getName().equals("test-0")));
    Assertions.assertFalse(context.getCluster().getStatus().getPodStatuses()
        .stream().filter(podStatus -> podStatus.getName().equals("test-0"))
        .findAny().map(StackGresClusterPodStatus::getPendingRestart).orElse(false));
    Assertions.assertIterableEquals(ImmutableList.of(installedExtension),
        context.getCluster().getStatus().getPodStatuses()
            .stream().filter(podStatus -> podStatus.getName().equals("test-0"))
            .findAny().map(StackGresClusterPodStatus::getInstalledPostgresExtensions)
            .stream().flatMap(List::stream).collect(ImmutableList.toImmutableList()));
    verify(extensionInstaller, times(1)).isExtensionInstalled();
    verify(extensionInstaller, times(1)).isLinksCreated();
    verify(extensionUninstaller, times(0)).isExtensionInstalled();
    verify(extensionInstaller, times(1)).getInstalledExtension();
    verify(extensionInstaller, times(0)).downloadAndExtract();
    verify(extensionInstaller, times(0)).verify();
    verify(extensionInstaller, times(0)).installExtension();
    verify(extensionInstaller, times(0)).createExtensionLinks();
    verify(extensionInstaller, times(0)).doesInstallOverwriteAnySharedLibrary();
    verify(extensionInstaller, times(0)).setExtensionAsPending();
    verify(extensionUninstaller, times(0)).uninstallExtension();
  }

  @Test
  void testReconciliationWithExtAlreadyPresentButLinksNotCreated_installIsSkippedButLinksCreated()
      throws Exception {
    StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    StackGresClusterExtension extension = new StackGresClusterExtension();
    when(extensionManager.getExtensionInstaller(any(), any(StackGresClusterExtension.class)))
        .thenReturn(extensionInstaller);
    when(extensionInstaller
        .isExtensionInstalled())
            .thenReturn(true);
    when(extensionInstaller
        .isLinksCreated())
            .thenReturn(false);
    when(extensionInstaller
        .getInstalledExtension())
            .thenReturn(installedExtension);
    ExtensionReconciliatorContext context = getContext(cluster -> {
      extension.setName("timescaledb");
      cluster.getSpec().setPostgresExtensions(new ArrayList<>());
      cluster.getSpec().getPostgresExtensions().add(extension);
    });
    Assertions.assertTrue(reconciliator.reconcile(null, context).result().get());
    Assertions.assertTrue(Optional.of(context.getCluster())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getPodStatuses)
        .stream().flatMap(List::stream)
        .anyMatch(podStatus -> podStatus.getName().equals("test-0")));
    Assertions.assertFalse(context.getCluster().getStatus().getPodStatuses()
        .stream().filter(podStatus -> podStatus.getName().equals("test-0"))
        .findAny().map(StackGresClusterPodStatus::getPendingRestart).orElse(false));
    Assertions.assertIterableEquals(ImmutableList.of(installedExtension),
        context.getCluster().getStatus().getPodStatuses()
            .stream().filter(podStatus -> podStatus.getName().equals("test-0"))
            .findAny().map(StackGresClusterPodStatus::getInstalledPostgresExtensions)
            .stream().flatMap(List::stream).collect(ImmutableList.toImmutableList()));
    verify(extensionInstaller, times(1)).isExtensionInstalled();
    verify(extensionInstaller, times(1)).isLinksCreated();
    verify(extensionUninstaller, times(0)).isExtensionInstalled();
    verify(extensionInstaller, times(1)).getInstalledExtension();
    verify(extensionInstaller, times(0)).downloadAndExtract();
    verify(extensionInstaller, times(0)).verify();
    verify(extensionInstaller, times(0)).installExtension();
    verify(extensionInstaller, times(1)).createExtensionLinks();
    verify(extensionInstaller, times(0)).doesInstallOverwriteAnySharedLibrary();
    verify(extensionInstaller, times(0)).setExtensionAsPending();
    verify(extensionUninstaller, times(0)).uninstallExtension();
  }

  @Test
  void testInitReconciliationWithExtensionThatOverwrite_installIsPerformed() throws Exception {
    StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    StackGresClusterExtension extension = getExtension();
    when(extensionManager.getExtensionInstaller(any(), any(StackGresClusterExtension.class)))
        .thenReturn(extensionInstaller);
    when(extensionInstaller
        .isExtensionInstalled())
            .thenReturn(false);
    when(extensionInstaller
        .getInstalledExtension())
            .thenReturn(installedExtension);
    ExtensionReconciliatorContext context = getContext(cluster -> {
      cluster.getSpec().setPostgresExtensions(new ArrayList<>());
      cluster.getSpec().getPostgresExtensions().add(extension);
    });
    Assertions.assertTrue(initReconciliator.reconcile(null, context).result().get());
    Assertions.assertTrue(Optional.of(context.getCluster())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getPodStatuses)
        .stream().flatMap(List::stream)
        .anyMatch(podStatus -> podStatus.getName().equals("test-0")));
    Assertions.assertFalse(context.getCluster().getStatus().getPodStatuses()
        .stream().filter(podStatus -> podStatus.getName().equals("test-0"))
        .findAny().map(StackGresClusterPodStatus::getPendingRestart).orElse(false));
    Assertions.assertIterableEquals(ImmutableList.of(installedExtension),
        context.getCluster().getStatus().getPodStatuses()
            .stream().filter(podStatus -> podStatus.getName().equals("test-0"))
            .findAny().map(StackGresClusterPodStatus::getInstalledPostgresExtensions)
            .stream().flatMap(List::stream).collect(ImmutableList.toImmutableList()));
    verify(extensionInstaller, times(1)).isExtensionInstalled();
    verify(extensionInstaller, times(0)).isLinksCreated();
    verify(extensionInstaller, times(0)).isExtensionPendingOverwrite();
    verify(extensionUninstaller, times(0)).isExtensionInstalled();
    verify(extensionInstaller, times(1)).getInstalledExtension();
    verify(extensionInstaller, times(1)).downloadAndExtract();
    verify(extensionInstaller, times(1)).verify();
    verify(extensionInstaller, times(1)).installExtension();
    verify(extensionInstaller, times(0)).createExtensionLinks();
    verify(extensionInstaller, times(0)).doesInstallOverwriteAnySharedLibrary();
    verify(extensionInstaller, times(0)).setExtensionAsPending();
    verify(extensionUninstaller, times(0)).uninstallExtension();
  }

  @Test
  void testReconciliationWithExtensionThatOverwrite_installIsSkipped() throws Exception {
    StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    StackGresClusterExtension extension = getExtension();
    when(extensionManager.getExtensionInstaller(any(), any(StackGresClusterExtension.class)))
        .thenReturn(extensionInstaller);
    when(extensionInstaller
        .isExtensionInstalled())
            .thenReturn(false);
    when(extensionInstaller
        .isExtensionPendingOverwrite())
            .thenReturn(false);
    when(extensionInstaller
        .doesInstallOverwriteAnySharedLibrary())
            .thenReturn(true);
    when(extensionInstaller
        .getInstalledExtension())
            .thenReturn(installedExtension);
    ExtensionReconciliatorContext context = getContext(cluster -> {
      cluster.getSpec().setPostgresExtensions(new ArrayList<>());
      cluster.getSpec().getPostgresExtensions().add(extension);
    });
    Assertions.assertTrue(reconciliator.reconcile(null, context).result().get());
    Assertions.assertTrue(Optional.of(context.getCluster())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getPodStatuses)
        .stream().flatMap(List::stream)
        .anyMatch(podStatus -> podStatus.getName().equals("test-0")));
    Assertions.assertTrue(context.getCluster().getStatus().getPodStatuses()
        .stream().filter(podStatus -> podStatus.getName().equals("test-0"))
        .findAny().map(StackGresClusterPodStatus::getPendingRestart).orElse(false));
    Assertions.assertIterableEquals(ImmutableList.of(installedExtension),
        context.getCluster().getStatus().getPodStatuses()
            .stream().filter(podStatus -> podStatus.getName().equals("test-0"))
            .findAny().map(StackGresClusterPodStatus::getInstalledPostgresExtensions)
            .stream().flatMap(List::stream).collect(ImmutableList.toImmutableList()));
    verify(extensionInstaller, times(1)).isExtensionInstalled();
    verify(extensionInstaller, times(0)).isLinksCreated();
    verify(extensionInstaller, times(2)).isExtensionPendingOverwrite();
    verify(extensionUninstaller, times(0)).isExtensionInstalled();
    verify(extensionInstaller, times(1)).getInstalledExtension();
    verify(extensionInstaller, times(1)).downloadAndExtract();
    verify(extensionInstaller, times(1)).verify();
    verify(extensionInstaller, times(0)).installExtension();
    verify(extensionInstaller, times(0)).createExtensionLinks();
    verify(extensionInstaller, times(1)).doesInstallOverwriteAnySharedLibrary();
    verify(extensionInstaller, times(1)).setExtensionAsPending();
    verify(extensionUninstaller, times(0)).uninstallExtension();
  }

  @Test
  void testInitReconciliationWithExtensionPending_installIsPerformed() throws Exception {
    StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    StackGresClusterExtension extension = getExtension();
    when(extensionManager.areCompatibles(any(), same(extension), same(installedExtension)))
        .thenReturn(true);
    when(extensionManager.getExtensionInstaller(any(),
        any(StackGresClusterInstalledExtension.class)))
            .thenReturn(extensionInstaller);
    when(extensionInstaller
        .isExtensionInstalled())
            .thenReturn(false);
    when(extensionInstaller
        .getInstalledExtension())
            .thenReturn(installedExtension);
    when(extensionInstaller
        .getInstalledExtension())
            .thenReturn(installedExtension);
    ExtensionReconciliatorContext context = getContext(cluster -> {
      cluster.getSpec().setPostgresExtensions(new ArrayList<>());
      cluster.getSpec().getPostgresExtensions().add(extension);
      cluster.setStatus(new StackGresClusterStatus());
      cluster.getStatus().setPodStatuses(new ArrayList<>());
      StackGresClusterPodStatus podStatus = new StackGresClusterPodStatus();
      podStatus.setName("test-0");
      podStatus.setPendingRestart(true);
      podStatus.setInstalledPostgresExtensions(new ArrayList<>());
      podStatus.getInstalledPostgresExtensions().add(installedExtension);
      cluster.getStatus().getPodStatuses().add(podStatus);
    });
    Assertions.assertTrue(initReconciliator.reconcile(null, context).result().get());
    Assertions.assertTrue(Optional.of(context.getCluster())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getPodStatuses)
        .stream().flatMap(List::stream)
        .anyMatch(podStatus -> podStatus.getName().equals("test-0")));
    Assertions.assertFalse(context.getCluster().getStatus().getPodStatuses()
        .stream().filter(podStatus -> podStatus.getName().equals("test-0"))
        .findAny().map(StackGresClusterPodStatus::getPendingRestart).orElse(false));
    Assertions.assertIterableEquals(ImmutableList.of(installedExtension),
        context.getCluster().getStatus().getPodStatuses()
            .stream().filter(podStatus -> podStatus.getName().equals("test-0"))
            .findAny().map(StackGresClusterPodStatus::getInstalledPostgresExtensions)
            .stream().flatMap(List::stream).collect(ImmutableList.toImmutableList()));
    verify(extensionInstaller, times(1)).isExtensionInstalled();
    verify(extensionInstaller, times(0)).isLinksCreated();
    verify(extensionInstaller, times(0)).isExtensionPendingOverwrite();
    verify(extensionUninstaller, times(0)).isExtensionInstalled();
    verify(extensionInstaller, times(1)).getInstalledExtension();
    verify(extensionInstaller, times(1)).downloadAndExtract();
    verify(extensionInstaller, times(1)).verify();
    verify(extensionInstaller, times(1)).installExtension();
    verify(extensionInstaller, times(0)).createExtensionLinks();
    verify(extensionInstaller, times(0)).doesInstallOverwriteAnySharedLibrary();
    verify(extensionInstaller, times(0)).setExtensionAsPending();
    verify(extensionUninstaller, times(0)).uninstallExtension();
  }

  @Test
  void testReconciliationWithExtensionPending_installIsSkipped() throws Exception {
    StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    StackGresClusterExtension extension = getExtension();
    when(extensionManager.areCompatibles(any(), same(extension), same(installedExtension)))
        .thenReturn(true);
    when(extensionManager.getExtensionInstaller(any(),
        any(StackGresClusterInstalledExtension.class)))
            .thenReturn(extensionInstaller);
    when(extensionInstaller
        .isExtensionInstalled())
            .thenReturn(false);
    when(extensionInstaller
        .isLinksCreated())
            .thenReturn(true);
    when(extensionInstaller
        .isExtensionPendingOverwrite())
            .thenReturn(true);
    when(extensionInstaller
        .getInstalledExtension())
            .thenReturn(installedExtension);
    ExtensionReconciliatorContext context = getContext(cluster -> {
      cluster.getSpec().setPostgresExtensions(new ArrayList<>());
      cluster.getSpec().getPostgresExtensions().add(extension);
      cluster.setStatus(new StackGresClusterStatus());
      cluster.getStatus().setPodStatuses(new ArrayList<>());
      StackGresClusterPodStatus podStatus = new StackGresClusterPodStatus();
      podStatus.setName("test-0");
      podStatus.setPendingRestart(true);
      podStatus.setInstalledPostgresExtensions(new ArrayList<>());
      podStatus.getInstalledPostgresExtensions().add(installedExtension);
      cluster.getStatus().getPodStatuses().add(podStatus);
    });
    Assertions.assertFalse(reconciliator.reconcile(null, context).result().get());
    Assertions.assertTrue(Optional.of(context.getCluster())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getPodStatuses)
        .stream().flatMap(List::stream)
        .anyMatch(podStatus -> podStatus.getName().equals("test-0")));
    Assertions.assertTrue(context.getCluster().getStatus().getPodStatuses()
        .stream().filter(podStatus -> podStatus.getName().equals("test-0"))
        .findAny().map(StackGresClusterPodStatus::getPendingRestart).orElse(false));
    Assertions.assertIterableEquals(ImmutableList.of(installedExtension),
        context.getCluster().getStatus().getPodStatuses()
            .stream().filter(podStatus -> podStatus.getName().equals("test-0"))
            .findAny().map(StackGresClusterPodStatus::getInstalledPostgresExtensions)
            .stream().flatMap(List::stream).collect(ImmutableList.toImmutableList()));
    verify(extensionInstaller, times(1)).isExtensionInstalled();
    verify(extensionInstaller, times(1)).isLinksCreated();
    verify(extensionInstaller, times(1)).isExtensionPendingOverwrite();
    verify(extensionUninstaller, times(0)).isExtensionInstalled();
    verify(extensionInstaller, times(1)).getInstalledExtension();
    verify(extensionInstaller, times(0)).downloadAndExtract();
    verify(extensionInstaller, times(0)).verify();
    verify(extensionInstaller, times(0)).installExtension();
    verify(extensionInstaller, times(0)).createExtensionLinks();
    verify(extensionInstaller, times(0)).doesInstallOverwriteAnySharedLibrary();
    verify(extensionInstaller, times(0)).setExtensionAsPending();
    verify(extensionUninstaller, times(0)).uninstallExtension();
  }

  @Test
  void testReconciliationWithExtensionAlreadyInstalled_installIsSkipped() throws Exception {
    StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    StackGresClusterExtension extension = getExtension();
    when(extensionManager.areCompatibles(any(), same(extension), same(installedExtension)))
        .thenReturn(true);
    when(extensionManager.getExtensionInstaller(any(),
        any(StackGresClusterInstalledExtension.class)))
            .thenReturn(extensionInstaller);
    when(extensionInstaller
        .isExtensionInstalled())
            .thenReturn(true);
    when(extensionInstaller
        .isLinksCreated())
            .thenReturn(true);
    when(extensionInstaller
        .getInstalledExtension())
            .thenReturn(installedExtension);
    ExtensionReconciliatorContext context = getContext(cluster -> {
      cluster.getSpec().setPostgresExtensions(new ArrayList<>());
      cluster.getSpec().getPostgresExtensions().add(extension);
      cluster.setStatus(new StackGresClusterStatus());
      cluster.getStatus().setPodStatuses(new ArrayList<>());
      StackGresClusterPodStatus podStatus = new StackGresClusterPodStatus();
      podStatus.setName("test-0");
      podStatus.setInstalledPostgresExtensions(new ArrayList<>());
      podStatus.getInstalledPostgresExtensions().add(installedExtension);
      cluster.getStatus().getPodStatuses().add(podStatus);
    });
    Assertions.assertFalse(reconciliator.reconcile(null, context).result().get());
    Assertions.assertTrue(Optional.of(context.getCluster())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getPodStatuses)
        .stream().flatMap(List::stream)
        .anyMatch(podStatus -> podStatus.getName().equals("test-0")));
    Assertions.assertFalse(context.getCluster().getStatus().getPodStatuses()
        .stream().filter(podStatus -> podStatus.getName().equals("test-0"))
        .findAny().map(StackGresClusterPodStatus::getPendingRestart).orElse(false));
    Assertions.assertIterableEquals(ImmutableList.of(installedExtension),
        context.getCluster().getStatus().getPodStatuses()
            .stream().filter(podStatus -> podStatus.getName().equals("test-0"))
            .findAny().map(StackGresClusterPodStatus::getInstalledPostgresExtensions)
            .stream().flatMap(List::stream).collect(ImmutableList.toImmutableList()));
    verify(extensionInstaller, times(1)).isExtensionInstalled();
    verify(extensionInstaller, times(1)).isLinksCreated();
    verify(extensionUninstaller, times(0)).isExtensionInstalled();
    verify(extensionInstaller, times(1)).getInstalledExtension();
    verify(extensionInstaller, times(0)).downloadAndExtract();
    verify(extensionInstaller, times(0)).verify();
    verify(extensionInstaller, times(0)).installExtension();
    verify(extensionInstaller, times(0)).createExtensionLinks();
    verify(extensionInstaller, times(0)).createExtensionLinks();
    verify(extensionInstaller, times(0)).doesInstallOverwriteAnySharedLibrary();
    verify(extensionInstaller, times(0)).setExtensionAsPending();
    verify(extensionUninstaller, times(0)).uninstallExtension();
  }

  @Test
  void testReconciliationWithPreviousExtensionAlreadyInstalled_upgradeIsPerformed()
      throws Exception {
    StackGresClusterExtension extension = getExtension();
    StackGresClusterInstalledExtension previousInstalledExtension = getInstalledExtension();
    previousInstalledExtension.setVersion("1.7.0");
    StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    when(extensionManager.areCompatibles(any(), same(extension), same(previousInstalledExtension)))
        .thenReturn(false);
    when(extensionManager.getExtensionInstaller(any(), any(StackGresClusterExtension.class)))
        .thenReturn(extensionInstaller);
    when(extensionInstaller
        .isExtensionPendingOverwrite())
            .thenReturn(false);
    when(extensionInstaller
        .isExtensionInstalled())
            .thenReturn(false);
    when(extensionInstaller
        .getInstalledExtension())
            .thenReturn(installedExtension);
    when(extensionInstaller
        .getInstalledExtension())
            .thenReturn(installedExtension);
    ExtensionReconciliatorContext context = getContext(cluster -> {
      cluster.getSpec().setPostgresExtensions(new ArrayList<>());
      cluster.getSpec().getPostgresExtensions().add(extension);
      cluster.setStatus(new StackGresClusterStatus());
      cluster.getStatus().setPodStatuses(new ArrayList<>());
      StackGresClusterPodStatus podStatus = new StackGresClusterPodStatus();
      podStatus.setName("test-0");
      podStatus.setInstalledPostgresExtensions(new ArrayList<>());
      podStatus.getInstalledPostgresExtensions().add(previousInstalledExtension);
      cluster.getStatus().getPodStatuses().add(podStatus);
    });
    Assertions.assertTrue(reconciliator.reconcile(null, context).result().get());
    Assertions.assertTrue(Optional.of(context.getCluster())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getPodStatuses)
        .stream().flatMap(List::stream)
        .anyMatch(podStatus -> podStatus.getName().equals("test-0")));
    Assertions.assertFalse(context.getCluster().getStatus().getPodStatuses()
        .stream().filter(podStatus -> podStatus.getName().equals("test-0"))
        .findAny().map(StackGresClusterPodStatus::getPendingRestart).orElse(false));
    Assertions.assertIterableEquals(ImmutableList.of(installedExtension),
        context.getCluster().getStatus().getPodStatuses()
            .stream().filter(podStatus -> podStatus.getName().equals("test-0"))
            .findAny().map(StackGresClusterPodStatus::getInstalledPostgresExtensions)
            .stream().flatMap(List::stream).collect(ImmutableList.toImmutableList()));
    verify(extensionInstaller, times(1)).isExtensionInstalled();
    verify(extensionInstaller, times(0)).isLinksCreated();
    verify(extensionUninstaller, times(0)).isExtensionInstalled();
    verify(extensionInstaller, times(1)).getInstalledExtension();
    verify(extensionInstaller, times(1)).downloadAndExtract();
    verify(extensionInstaller, times(1)).verify();
    verify(extensionInstaller, times(1)).installExtension();
    verify(extensionInstaller, times(0)).createExtensionLinks();
    verify(extensionInstaller, times(1)).doesInstallOverwriteAnySharedLibrary();
    verify(extensionInstaller, times(0)).setExtensionAsPending();
    verify(extensionUninstaller, times(0)).uninstallExtension();
  }

  @Test
  void testInitReconciliationWithInstalledExtensions_uninstallIsPerformed() throws Exception {
    ExtensionReconciliatorContext context = getContext(cluster -> {
      cluster.setStatus(new StackGresClusterStatus());
      cluster.getStatus().setPodStatuses(new ArrayList<>());
      StackGresClusterPodStatus podStatus = new StackGresClusterPodStatus();
      podStatus.setName("test-0");
      podStatus.setInstalledPostgresExtensions(new ArrayList<>());
      StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
      podStatus.getInstalledPostgresExtensions().add(installedExtension);
      cluster.getStatus().getPodStatuses().add(podStatus);
    });
    when(extensionManager.getExtensionUninstaller(any(),
        any(StackGresClusterInstalledExtension.class)))
            .thenReturn(extensionUninstaller);
    when(extensionUninstaller
        .isExtensionInstalled())
            .thenReturn(true);
    Assertions.assertTrue(initReconciliator.reconcile(null, context).result().get());
    Assertions.assertTrue(Optional.of(context.getCluster())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getPodStatuses)
        .stream().flatMap(List::stream)
        .anyMatch(podStatus -> podStatus.getName().equals("test-0")));
    Assertions.assertFalse(context.getCluster().getStatus().getPodStatuses()
        .stream().filter(podStatus -> podStatus.getName().equals("test-0"))
        .findAny().map(StackGresClusterPodStatus::getPendingRestart).orElse(false));
    Assertions.assertIterableEquals(ImmutableList.of(),
        context.getCluster().getStatus().getPodStatuses()
            .stream().filter(podStatus -> podStatus.getName().equals("test-0"))
            .findAny().map(StackGresClusterPodStatus::getInstalledPostgresExtensions)
            .stream().flatMap(List::stream).collect(ImmutableList.toImmutableList()));
    verify(extensionInstaller, times(0)).isExtensionInstalled();
    verify(extensionInstaller, times(0)).isLinksCreated();
    verify(extensionUninstaller, times(1)).isExtensionInstalled();
    verify(extensionInstaller, times(0)).getInstalledExtension();
    verify(extensionInstaller, times(0)).downloadAndExtract();
    verify(extensionInstaller, times(0)).verify();
    verify(extensionInstaller, times(0)).installExtension();
    verify(extensionInstaller, times(0)).createExtensionLinks();
    verify(extensionInstaller, times(0)).createExtensionLinks();
    verify(extensionInstaller, times(0)).doesInstallOverwriteAnySharedLibrary();
    verify(extensionInstaller, times(0)).setExtensionAsPending();
    verify(extensionUninstaller, times(1)).uninstallExtension();
  }

  @Test
  void testReconciliationWithInstalledExtensions_uninstallIsSkipped() throws Exception {
    final StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    ExtensionReconciliatorContext context = getContext(cluster -> {
      cluster.setStatus(new StackGresClusterStatus());
      cluster.getStatus().setPodStatuses(new ArrayList<>());
      StackGresClusterPodStatus podStatus = new StackGresClusterPodStatus();
      podStatus.setName("test-0");
      podStatus.setInstalledPostgresExtensions(new ArrayList<>());
      podStatus.getInstalledPostgresExtensions().add(installedExtension);
      cluster.getStatus().getPodStatuses().add(podStatus);
    });
    Assertions.assertTrue(reconciliator.reconcile(null, context).result().get());
    Assertions.assertTrue(Optional.of(context.getCluster())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getPodStatuses)
        .stream().flatMap(List::stream)
        .anyMatch(podStatus -> podStatus.getName().equals("test-0")));
    Assertions.assertTrue(context.getCluster().getStatus().getPodStatuses()
        .stream().filter(podStatus -> podStatus.getName().equals("test-0"))
        .findAny().map(StackGresClusterPodStatus::getPendingRestart).orElse(false));
    Assertions.assertIterableEquals(ImmutableList.of(installedExtension),
        context.getCluster().getStatus().getPodStatuses()
            .stream().filter(podStatus -> podStatus.getName().equals("test-0"))
            .findAny().map(StackGresClusterPodStatus::getInstalledPostgresExtensions)
            .stream().flatMap(List::stream).collect(ImmutableList.toImmutableList()));
    verify(extensionInstaller, times(0)).isExtensionInstalled();
    verify(extensionInstaller, times(0)).isLinksCreated();
    verify(extensionUninstaller, times(0)).isExtensionInstalled();
    verify(extensionInstaller, times(0)).getInstalledExtension();
    verify(extensionInstaller, times(0)).downloadAndExtract();
    verify(extensionInstaller, times(0)).verify();
    verify(extensionInstaller, times(0)).installExtension();
    verify(extensionInstaller, times(0)).createExtensionLinks();
    verify(extensionInstaller, times(0)).createExtensionLinks();
    verify(extensionInstaller, times(0)).doesInstallOverwriteAnySharedLibrary();
    verify(extensionInstaller, times(0)).setExtensionAsPending();
    verify(extensionUninstaller, times(0)).uninstallExtension();
  }

  @Test
  void testReconciliationWithoutExtensions_uninstallIsSkipped() throws Exception {
    ExtensionReconciliatorContext context = getContext(cluster -> {
      cluster.getSpec().setPostgresExtensions(null);
    });
    Assertions.assertFalse(reconciliator.reconcile(null, context).result().get());
    Assertions.assertTrue(Optional.of(context.getCluster())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getPodStatuses)
        .stream().flatMap(List::stream)
        .anyMatch(podStatus -> podStatus.getName().equals("test-0")));
    Assertions.assertFalse(context.getCluster().getStatus().getPodStatuses()
        .stream().filter(podStatus -> podStatus.getName().equals("test-0"))
        .findAny().map(StackGresClusterPodStatus::getPendingRestart).orElse(false));
    Assertions.assertIterableEquals(ImmutableList.of(),
        context.getCluster().getStatus().getPodStatuses()
            .stream().filter(podStatus -> podStatus.getName().equals("test-0"))
            .findAny().map(StackGresClusterPodStatus::getInstalledPostgresExtensions)
            .stream().flatMap(List::stream).collect(ImmutableList.toImmutableList()));
    verify(extensionInstaller, times(0)).isExtensionInstalled();
    verify(extensionInstaller, times(0)).isLinksCreated();
    verify(extensionUninstaller, times(0)).isExtensionInstalled();
    verify(extensionInstaller, times(0)).getInstalledExtension();
    verify(extensionInstaller, times(0)).downloadAndExtract();
    verify(extensionInstaller, times(0)).verify();
    verify(extensionInstaller, times(0)).installExtension();
    verify(extensionInstaller, times(0)).createExtensionLinks();
    verify(extensionInstaller, times(0)).doesInstallOverwriteAnySharedLibrary();
    verify(extensionInstaller, times(0)).setExtensionAsPending();
    verify(extensionUninstaller, times(0)).uninstallExtension();
  }

}
