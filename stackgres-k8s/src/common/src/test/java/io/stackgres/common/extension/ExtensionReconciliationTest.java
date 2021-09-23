/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
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
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.extension.ExtensionManager.ExtensionInstaller;
import io.stackgres.common.extension.ExtensionManager.ExtensionUninstaller;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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

  @Mock
  private ExtensionEventEmitter eventEmitter;

  private ExtensionReconciliator<ExtensionReconciliatorContext> initReconciliator;

  private ExtensionReconciliator<ExtensionReconciliatorContext> reconciliator;

  @BeforeEach
  void setUp() throws Exception {
    initReconciliator = new ExtensionReconciliator<>("test-0",
        extensionManager, false, eventEmitter) {
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
    reconciliator = new ExtensionReconciliator<>("test-0",
        extensionManager, true, eventEmitter) {
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
    cluster.getSpec().getPostgres().setVersion(POSTGRES_VERSION);
    consumer.accept(cluster);
    when(context.getCluster()).thenReturn(cluster);
    when(context.getExtensions()).thenReturn(
        Optional.ofNullable(cluster.getSpec())
            .map(StackGresClusterSpec::getToInstallPostgresExtensions)
            .map(ImmutableList::copyOf)
            .orElse(ImmutableList.of()));
    return context;
  }

  @Test
  void testReconciliationWithExtension_installIsPerformed() throws Exception {
    StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    ExtensionReconciliatorContext context = getContext(cluster -> {
      cluster.getSpec().getPostgres().setExtensions(null);
      cluster.getSpec().setToInstallPostgresExtensions(new ArrayList<>());
      cluster.getSpec().getToInstallPostgresExtensions().add(installedExtension);
    });
    when(extensionManager.getExtensionInstaller(
        any(), any(StackGresClusterInstalledExtension.class)))
        .thenReturn(extensionInstaller);
    when(extensionInstaller
        .isExtensionInstalled())
        .thenReturn(false);
    when(extensionInstaller
        .isExtensionPendingOverwrite())
        .thenReturn(false);
    doNothing().when(eventEmitter).emitExtensionDeployed(installedExtension);
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
    verify(extensionInstaller, times(1)).downloadAndExtract();
    verify(extensionInstaller, times(1)).verify();
    verify(extensionInstaller, times(1)).installExtension();
    verify(extensionInstaller, times(0)).createExtensionLinks();
    verify(extensionInstaller, times(1)).doesInstallOverwriteAnySharedFile();
    verify(extensionInstaller, times(0)).setExtensionAsPending();
    verify(extensionUninstaller, times(0)).uninstallExtension();
    verify(eventEmitter).emitExtensionDeployed(installedExtension);
  }

  @Test
  void testReconciliationWithExtensionAlreadyPresent_installIsSkippedButStatusUpdated()
      throws Exception {
    StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    ExtensionReconciliatorContext context = getContext(cluster -> {
      cluster.getSpec().getPostgres().setExtensions(null);
      cluster.getSpec().setToInstallPostgresExtensions(new ArrayList<>());
      cluster.getSpec().getToInstallPostgresExtensions().add(installedExtension);
    });
    when(extensionManager.getExtensionInstaller(
        any(), any(StackGresClusterInstalledExtension.class)))
        .thenReturn(extensionInstaller);
    when(extensionInstaller
        .isExtensionInstalled())
        .thenReturn(true);
    when(extensionInstaller
        .isLinksCreated())
        .thenReturn(true);
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
    verify(extensionInstaller, times(0)).downloadAndExtract();
    verify(extensionInstaller, times(0)).verify();
    verify(extensionInstaller, times(0)).installExtension();
    verify(extensionInstaller, times(0)).createExtensionLinks();
    verify(extensionInstaller, times(0)).doesInstallOverwriteAnySharedFile();
    verify(extensionInstaller, times(0)).setExtensionAsPending();
    verify(extensionUninstaller, times(0)).uninstallExtension();
  }

  @Test
  void testReconciliationWithExtAlreadyPresentButLinksNotCreated_installIsSkippedButLinksCreated()
      throws Exception {
    StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    ExtensionReconciliatorContext context = getContext(cluster -> {
      cluster.getSpec().getPostgres().setExtensions(null);
      cluster.getSpec().setToInstallPostgresExtensions(new ArrayList<>());
      cluster.getSpec().getToInstallPostgresExtensions().add(installedExtension);
      cluster.setStatus(new StackGresClusterStatus());
      cluster.getStatus().setPodStatuses(new ArrayList<>());
      StackGresClusterPodStatus podStatus = new StackGresClusterPodStatus();
      podStatus.setName("test-0");
      podStatus.setInstalledPostgresExtensions(new ArrayList<>());
      podStatus.getInstalledPostgresExtensions().add(installedExtension);
      cluster.getStatus().getPodStatuses().add(podStatus);
    });
    when(extensionManager.getExtensionInstaller(
        any(), any(StackGresClusterInstalledExtension.class)))
        .thenReturn(extensionInstaller);
    when(extensionInstaller
        .isExtensionInstalled())
        .thenReturn(true);
    when(extensionInstaller
        .isLinksCreated())
        .thenReturn(false);
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
    verify(extensionInstaller, times(0)).downloadAndExtract();
    verify(extensionInstaller, times(0)).verify();
    verify(extensionInstaller, times(0)).installExtension();
    verify(extensionInstaller, times(1)).createExtensionLinks();
    verify(extensionInstaller, times(0)).doesInstallOverwriteAnySharedFile();
    verify(extensionInstaller, times(0)).setExtensionAsPending();
    verify(extensionUninstaller, times(0)).uninstallExtension();
  }

  @Test
  void testInitReconciliationWithExtensionThatOverwrite_installIsPerformed() throws Exception {
    StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    ExtensionReconciliatorContext context = getContext(cluster -> {
      cluster.getSpec().getPostgres().setExtensions(null);
      cluster.getSpec().setToInstallPostgresExtensions(new ArrayList<>());
      cluster.getSpec().getToInstallPostgresExtensions().add(installedExtension);
    });
    when(extensionManager.getExtensionInstaller(
        any(), any(StackGresClusterInstalledExtension.class)))
        .thenReturn(extensionInstaller);
    when(extensionInstaller
        .isExtensionInstalled())
        .thenReturn(false);
    doNothing().when(eventEmitter).emitExtensionDeployed(installedExtension);
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
    verify(extensionInstaller, times(1)).downloadAndExtract();
    verify(extensionInstaller, times(1)).verify();
    verify(extensionInstaller, times(1)).installExtension();
    verify(extensionInstaller, times(0)).createExtensionLinks();
    verify(extensionInstaller, times(0)).doesInstallOverwriteAnySharedFile();
    verify(extensionInstaller, times(0)).setExtensionAsPending();
    verify(extensionUninstaller, times(0)).uninstallExtension();
    verify(eventEmitter).emitExtensionDeployed(installedExtension);
  }

  @Test
  void testReconciliationWithExtensionThatOverwrite_installIsSkipped() throws Exception {
    StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    when(extensionManager.getExtensionInstaller(
        any(), any(StackGresClusterInstalledExtension.class)))
        .thenReturn(extensionInstaller);
    when(extensionInstaller
        .isExtensionInstalled())
        .thenReturn(false);
    when(extensionInstaller
        .isExtensionPendingOverwrite())
        .thenReturn(false);
    when(extensionInstaller
        .doesInstallOverwriteAnySharedFile())
        .thenReturn(true);
    ExtensionReconciliatorContext context = getContext(cluster -> {
      cluster.getSpec().getPostgres().setExtensions(null);
      cluster.getSpec().setToInstallPostgresExtensions(new ArrayList<>());
      cluster.getSpec().getToInstallPostgresExtensions().add(installedExtension);
    });
    doNothing().when(eventEmitter).emitExtensionDeployedRestart(installedExtension);
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
    verify(extensionInstaller, times(1)).downloadAndExtract();
    verify(extensionInstaller, times(1)).verify();
    verify(extensionInstaller, times(0)).installExtension();
    verify(extensionInstaller, times(0)).createExtensionLinks();
    verify(extensionInstaller, times(1)).doesInstallOverwriteAnySharedFile();
    verify(extensionInstaller, times(1)).setExtensionAsPending();
    verify(extensionUninstaller, times(0)).uninstallExtension();
    verify(eventEmitter).emitExtensionDeployedRestart(installedExtension);
  }

  @Test
  void testInitReconciliationWithExtensionPending_installIsPerformed() throws Exception {
    StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    ExtensionReconciliatorContext context = getContext(cluster -> {
      cluster.getSpec().getPostgres().setExtensions(null);
      cluster.getSpec().setToInstallPostgresExtensions(new ArrayList<>());
      cluster.getSpec().getToInstallPostgresExtensions().add(installedExtension);
      cluster.setStatus(new StackGresClusterStatus());
      cluster.getStatus().setPodStatuses(new ArrayList<>());
      StackGresClusterPodStatus podStatus = new StackGresClusterPodStatus();
      podStatus.setName("test-0");
      podStatus.setPendingRestart(true);
      podStatus.setInstalledPostgresExtensions(new ArrayList<>());
      podStatus.getInstalledPostgresExtensions().add(installedExtension);
      cluster.getStatus().getPodStatuses().add(podStatus);
    });
    when(extensionManager.getExtensionInstaller(
        any(), any(StackGresClusterInstalledExtension.class)))
        .thenReturn(extensionInstaller);
    when(extensionInstaller
        .isExtensionInstalled())
        .thenReturn(false);
    doNothing().when(eventEmitter).emitExtensionDeployed(installedExtension);

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
    verify(extensionInstaller, times(1)).downloadAndExtract();
    verify(extensionInstaller, times(1)).verify();
    verify(extensionInstaller, times(1)).installExtension();
    verify(extensionInstaller, times(0)).createExtensionLinks();
    verify(extensionInstaller, times(0)).doesInstallOverwriteAnySharedFile();
    verify(extensionInstaller, times(0)).setExtensionAsPending();
    verify(extensionUninstaller, times(0)).uninstallExtension();
    verify(eventEmitter).emitExtensionDeployed(installedExtension);
  }

  @Test
  void testReconciliationWithExtensionPending_installIsSkipped() throws Exception {
    StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    ExtensionReconciliatorContext context = getContext(cluster -> {
      cluster.getSpec().getPostgres().setExtensions(null);
      cluster.getSpec().setToInstallPostgresExtensions(new ArrayList<>());
      cluster.getSpec().getToInstallPostgresExtensions().add(installedExtension);
      cluster.setStatus(new StackGresClusterStatus());
      cluster.getStatus().setPodStatuses(new ArrayList<>());
      StackGresClusterPodStatus podStatus = new StackGresClusterPodStatus();
      podStatus.setName("test-0");
      podStatus.setPendingRestart(true);
      podStatus.setInstalledPostgresExtensions(new ArrayList<>());
      podStatus.getInstalledPostgresExtensions().add(installedExtension);
      cluster.getStatus().getPodStatuses().add(podStatus);
    });
    when(extensionManager.getExtensionInstaller(
        any(), any(StackGresClusterInstalledExtension.class)))
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
    verify(extensionInstaller, times(0)).downloadAndExtract();
    verify(extensionInstaller, times(0)).verify();
    verify(extensionInstaller, times(0)).installExtension();
    verify(extensionInstaller, times(0)).createExtensionLinks();
    verify(extensionInstaller, times(0)).doesInstallOverwriteAnySharedFile();
    verify(extensionInstaller, times(0)).setExtensionAsPending();
    verify(extensionUninstaller, times(0)).uninstallExtension();
  }

  @Test
  void testReconciliationWithExtensionAlreadyInstalled_installIsSkipped() throws Exception {
    StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    ExtensionReconciliatorContext context = getContext(cluster -> {
      cluster.getSpec().getPostgres().setExtensions(null);
      cluster.getSpec().setToInstallPostgresExtensions(new ArrayList<>());
      cluster.getSpec().getToInstallPostgresExtensions().add(installedExtension);
      cluster.setStatus(new StackGresClusterStatus());
      cluster.getStatus().setPodStatuses(new ArrayList<>());
      StackGresClusterPodStatus podStatus = new StackGresClusterPodStatus();
      podStatus.setName("test-0");
      podStatus.setInstalledPostgresExtensions(new ArrayList<>());
      podStatus.getInstalledPostgresExtensions().add(installedExtension);
      cluster.getStatus().getPodStatuses().add(podStatus);
    });
    when(extensionManager.getExtensionInstaller(
        any(), any(StackGresClusterInstalledExtension.class)))
        .thenReturn(extensionInstaller);
    when(extensionInstaller
        .isExtensionInstalled())
        .thenReturn(true);
    when(extensionInstaller
        .isLinksCreated())
        .thenReturn(true);
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
    verify(extensionInstaller, times(0)).downloadAndExtract();
    verify(extensionInstaller, times(0)).verify();
    verify(extensionInstaller, times(0)).installExtension();
    verify(extensionInstaller, times(0)).createExtensionLinks();
    verify(extensionInstaller, times(0)).createExtensionLinks();
    verify(extensionInstaller, times(0)).doesInstallOverwriteAnySharedFile();
    verify(extensionInstaller, times(0)).setExtensionAsPending();
    verify(extensionUninstaller, times(0)).uninstallExtension();
  }

  @Test
  void testReconciliationWithPreviousExtensionAlreadyInstalled_upgradeIsPerformed()
      throws Exception {
    StackGresClusterInstalledExtension previousInstalledExtension = getInstalledExtension();
    previousInstalledExtension.setVersion("1.7.0");
    StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    ExtensionReconciliatorContext context = getContext(cluster -> {
      cluster.getSpec().getPostgres().setExtensions(null);
      cluster.getSpec().setToInstallPostgresExtensions(new ArrayList<>());
      cluster.getSpec().getToInstallPostgresExtensions().add(installedExtension);
      cluster.setStatus(new StackGresClusterStatus());
      cluster.getStatus().setPodStatuses(new ArrayList<>());
      StackGresClusterPodStatus podStatus = new StackGresClusterPodStatus();
      podStatus.setName("test-0");
      podStatus.setInstalledPostgresExtensions(new ArrayList<>());
      podStatus.getInstalledPostgresExtensions().add(previousInstalledExtension);
      cluster.getStatus().getPodStatuses().add(podStatus);
    });
    when(extensionManager.getExtensionInstaller(
        any(), any(StackGresClusterInstalledExtension.class)))
        .thenReturn(extensionInstaller);
    when(extensionInstaller
        .isExtensionPendingOverwrite())
        .thenReturn(false);
    when(extensionInstaller
        .isExtensionInstalled())
        .thenReturn(false);
    doNothing().when(eventEmitter).emitExtensionChanged(previousInstalledExtension,
        installedExtension);
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
    verify(extensionInstaller, times(1)).downloadAndExtract();
    verify(extensionInstaller, times(1)).verify();
    verify(extensionInstaller, times(1)).installExtension();
    verify(extensionInstaller, times(0)).createExtensionLinks();
    verify(extensionInstaller, times(1)).doesInstallOverwriteAnySharedFile();
    verify(extensionInstaller, times(0)).setExtensionAsPending();
    verify(extensionUninstaller, times(0)).uninstallExtension();
    verify(eventEmitter).emitExtensionChanged(previousInstalledExtension, installedExtension);
  }

  @Test
  void testInitReconciliationWithInstalledExtensions_uninstallIsPerformed() throws Exception {
    StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    ExtensionReconciliatorContext context = getContext(cluster -> {
      cluster.getSpec().getPostgres().setExtensions(null);
      cluster.setStatus(new StackGresClusterStatus());
      cluster.getStatus().setPodStatuses(new ArrayList<>());
      StackGresClusterPodStatus podStatus = new StackGresClusterPodStatus();
      podStatus.setName("test-0");
      podStatus.setInstalledPostgresExtensions(new ArrayList<>());
      podStatus.getInstalledPostgresExtensions().add(installedExtension);
      cluster.getStatus().getPodStatuses().add(podStatus);
    });
    when(extensionManager.getExtensionUninstaller(any(),
        any(StackGresClusterInstalledExtension.class)))
        .thenReturn(extensionUninstaller);
    when(extensionUninstaller
        .isExtensionInstalled())
        .thenReturn(true);
    doNothing().when(eventEmitter).emitExtensionRemoved(installedExtension);

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
    verify(extensionInstaller, times(0)).downloadAndExtract();
    verify(extensionInstaller, times(0)).verify();
    verify(extensionInstaller, times(0)).installExtension();
    verify(extensionInstaller, times(0)).createExtensionLinks();
    verify(extensionInstaller, times(0)).createExtensionLinks();
    verify(extensionInstaller, times(0)).doesInstallOverwriteAnySharedFile();
    verify(extensionInstaller, times(0)).setExtensionAsPending();
    verify(extensionUninstaller, times(1)).uninstallExtension();
    verify(eventEmitter).emitExtensionRemoved(installedExtension);
  }

  @Test
  void testReconciliationWithInstalledExtensions_uninstallIsSkippedButStatusUpdated()
      throws Exception {
    final StackGresClusterInstalledExtension installedExtension = getInstalledExtension();
    ExtensionReconciliatorContext context = getContext(cluster -> {
      cluster.getSpec().getPostgres().setExtensions(null);
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
        .findAny().map(StackGresClusterPodStatus::getPendingRestart).orElse(true));
    Assertions.assertIterableEquals(ImmutableList.of(installedExtension),
        context.getCluster().getStatus().getPodStatuses()
            .stream().filter(podStatus -> podStatus.getName().equals("test-0"))
            .findAny().map(StackGresClusterPodStatus::getInstalledPostgresExtensions)
            .stream().flatMap(List::stream).collect(ImmutableList.toImmutableList()));
    verify(extensionInstaller, times(0)).isExtensionInstalled();
    verify(extensionInstaller, times(0)).isLinksCreated();
    verify(extensionUninstaller, times(0)).isExtensionInstalled();
    verify(extensionInstaller, times(0)).downloadAndExtract();
    verify(extensionInstaller, times(0)).verify();
    verify(extensionInstaller, times(0)).installExtension();
    verify(extensionInstaller, times(0)).createExtensionLinks();
    verify(extensionInstaller, times(0)).createExtensionLinks();
    verify(extensionInstaller, times(0)).doesInstallOverwriteAnySharedFile();
    verify(extensionInstaller, times(0)).setExtensionAsPending();
    verify(extensionUninstaller, times(0)).uninstallExtension();
  }

  @Test
  void testReconciliationWithoutExtensions_uninstallIsSkipped() throws Exception {
    ExtensionReconciliatorContext context = getContext(cluster -> {
      cluster.getSpec().getPostgres().setExtensions(null);
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
    verify(extensionInstaller, times(0)).downloadAndExtract();
    verify(extensionInstaller, times(0)).verify();
    verify(extensionInstaller, times(0)).installExtension();
    verify(extensionInstaller, times(0)).createExtensionLinks();
    verify(extensionInstaller, times(0)).doesInstallOverwriteAnySharedFile();
    verify(extensionInstaller, times(0)).setExtensionAsPending();
    verify(extensionUninstaller, times(0)).uninstallExtension();
  }

}
