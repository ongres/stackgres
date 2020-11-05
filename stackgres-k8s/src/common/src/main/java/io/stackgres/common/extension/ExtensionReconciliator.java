/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.extension.ExtensionManager.ExtensionInstaller;
import io.stackgres.common.extension.ExtensionManager.ExtensionUninstaller;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ExtensionReconciliator<T extends ExtensionReconciliatorContext> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionReconciliator.class);

  private final String podName;
  private final ExtensionManager extensionManager;
  private final boolean skipSharedLibrariesOverwrites;

  public ExtensionReconciliator(String podName, ExtensionManager extensionManager,
      boolean skipSharedLibrariesOverwrites) {
    this.podName = podName;
    this.extensionManager = extensionManager;
    this.skipSharedLibrariesOverwrites = skipSharedLibrariesOverwrites;
  }

  @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION",
      justification = "False positives")
  public ReconciliationResult<Boolean> reconcile(KubernetesClient client, T context)
      throws Exception {
    final ImmutableList.Builder<Exception> exceptions = ImmutableList.builder();
    final StackGresCluster cluster = context.getCluster();
    final ImmutableList<StackGresClusterExtension> extensions = context.getExtensions();
    if (cluster.getStatus() == null) {
      cluster.setStatus(new StackGresClusterStatus());
    }
    if (cluster.getStatus().getPodStatuses() == null) {
      cluster.getStatus().setPodStatuses(new ArrayList<>());
    }
    if (cluster.getStatus().getPodStatuses().stream()
        .noneMatch(podStatus -> podStatus.getName().equals(podName))) {
      StackGresClusterPodStatus podStatus = new StackGresClusterPodStatus();
      podStatus.setName(podName);
      podStatus.setInstalledPostgresExtensions(new ArrayList<>());
      cluster.getStatus().getPodStatuses().add(podStatus);
    }
    final StackGresClusterPodStatus podStatus = cluster.getStatus().getPodStatuses().stream()
        .filter(status -> status.getName().equals(podName))
        .findAny().get();
    if (podStatus.getInstalledPostgresExtensions() == null) {
      podStatus.setInstalledPostgresExtensions(new ArrayList<>());
    }
    final List<StackGresClusterInstalledExtension> installedExtensions =
        podStatus.getInstalledPostgresExtensions();
    LOGGER.info("Reconcile postgres extensions...");
    boolean clusterUpdated = false;
    for (StackGresClusterInstalledExtension installedExtension : installedExtensions
        .stream()
        .filter(installedExtension -> extensions.stream()
            .noneMatch(extension -> installedExtension.same(extension)))
        .collect(Collectors.toList())) {
      ExtensionUninstaller extensionUninstaller = extensionManager.getExtensionUninstaller(
          context, installedExtension);
      try {
        if (!skipSharedLibrariesOverwrites) {
          if (extensionUninstaller.isExtensionInstalled()) {
            LOGGER.info("Removing extension: {}",
                ExtensionUtil.getDescription(installedExtension));
            extensionUninstaller.uninstallExtension();
          }
          installedExtensions.remove(installedExtension);
          clusterUpdated = true;
        } else {
          LOGGER.info("Skip uninstallation of extension: {}",
              ExtensionUtil.getDescription(installedExtension));
          if (!Optional.ofNullable(podStatus.getPendingRestart()).orElse(false)) {
            podStatus.setPendingRestart(true);
            clusterUpdated = true;
          }
        }
      } catch (Exception ex) {
        exceptions.add(ex);
        onUninstallException(client, cluster, ExtensionUtil.getDescription(installedExtension),
            podName, ex);
      }
    }
    for (StackGresClusterExtension extension : extensions) {
      try {
        final ExtensionInstaller extensionInstaller = cluster.getStatus().getPodStatuses()
            .stream()
            .map(StackGresClusterPodStatus::getInstalledPostgresExtensions)
            .flatMap(List::stream)
            .filter(installedExtension -> extensionManager.areCompatibles(
                context, extension, installedExtension))
            .map(Unchecked.function(installedExtension -> extensionManager
                .getExtensionInstaller(context, installedExtension)))
            .findFirst()
            .orElseGet(Unchecked.supplier(() -> extensionManager.getExtensionInstaller(
                context, extension)));
        final StackGresClusterInstalledExtension installedExtension =
            extensionInstaller.getInstalledExtension();
        if (!extensionInstaller.isExtensionInstalled()
            && (!skipSharedLibrariesOverwrites
                || !extensionInstaller.isExtensionPendingOverwrite())) {
          LOGGER.info("Download extension: {}", ExtensionUtil.getDescription(cluster, extension));
          extensionInstaller.downloadAndExtract();
          LOGGER.info("Verify extension: {}", ExtensionUtil.getDescription(cluster, extension));
          extensionInstaller.verify();
          if (skipSharedLibrariesOverwrites
              && extensionInstaller.doesInstallOverwriteAnySharedLibrary()) {
            LOGGER.info("Skip installation of extension: {}",
                ExtensionUtil.getDescription(cluster, extension));
            if (!extensionInstaller.isExtensionPendingOverwrite()) {
              extensionInstaller.setExtensionAsPending();
            }
            if (!Optional.ofNullable(podStatus.getPendingRestart()).orElse(false)) {
              podStatus.setPendingRestart(true);
              clusterUpdated = true;
            }
          } else {
            LOGGER.info("Install extension: {}", ExtensionUtil.getDescription(cluster, extension));
            extensionInstaller.installExtension();
          }
        } else {
          if (!extensionInstaller.isLinksCreated()) {
            extensionInstaller.createExtensionLinks();
          }
        }
        if (installedExtensions
            .stream()
            .noneMatch(anInstalledExtension -> anInstalledExtension.equals(installedExtension))) {
          installedExtensions.stream()
              .filter(anInstalledExtension -> anInstalledExtension.same(installedExtension))
              .peek(previousInstalledExtension -> LOGGER.info("Extension upgraded: {}",
                  ExtensionUtil.getDescription(previousInstalledExtension)))
              .findAny()
              .ifPresent(installedExtensions::remove);
          installedExtensions.add(installedExtension);
          clusterUpdated = true;
        }
      } catch (Exception ex) {
        exceptions.add(ex);
        onInstallException(client, cluster, ExtensionUtil.getDescription(cluster, extension),
            podName, ex);
      }
    }
    if (!skipSharedLibrariesOverwrites
        && Optional.ofNullable(podStatus.getPendingRestart()).orElse(false)) {
      podStatus.setPendingRestart(false);
      clusterUpdated = true;
    }
    return new ReconciliationResult<>(clusterUpdated, exceptions.build());
  }

  protected abstract void onUninstallException(KubernetesClient client, StackGresCluster cluster,
      String extension, String podName, Exception ex);

  protected abstract void onInstallException(KubernetesClient client, StackGresCluster cluster,
      String extension, String podName, Exception ex);

}
