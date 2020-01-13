/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.google.common.io.Resources;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.controller.ClusterReconciliationCycle;
import io.stackgres.operator.controller.ClusterResourceWatcherFactory;
import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupDefinition;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupDoneable;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupList;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfigDefinition;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfigDoneable;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfigList;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterDefinition;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterDoneable;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterList;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfigDefinition;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfigDoneable;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfigList;
import io.stackgres.operator.customresource.sgprofile.StackGresProfile;
import io.stackgres.operator.customresource.sgprofile.StackGresProfileDefinition;
import io.stackgres.operator.customresource.sgprofile.StackGresProfileDoneable;
import io.stackgres.operator.customresource.sgprofile.StackGresProfileList;
import io.stackgres.operator.resource.ResourceUtil;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigDefinition;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigDoneable;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigList;
import io.stackgres.operatorframework.resource.ResourceHandlerSelector;

import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StackGresOperatorApp {

  private static final Logger LOGGER = LoggerFactory.getLogger(StackGresOperatorApp.class);

  private final KubernetesClientFactory kubeClient;
  private final ResourceHandlerSelector<StackGresClusterContext> handlerSelector;
  private final ClusterReconciliationCycle clusterReconciliationCycle;
  private final ClusterResourceWatcherFactory watcherFactory;
  private final ScheduledExecutorService scheduledExecutorService =
      Executors.newScheduledThreadPool(1, r -> new Thread(r, "ClusterControllerShceduler"));
  private final List<Watch> watches = new ArrayList<>();

  /**
   * Create a {@code StackGresOperatorApp} instance.
   */
  @Inject
  public StackGresOperatorApp(KubernetesClientFactory kubeClient,
      ResourceHandlerSelector<StackGresClusterContext> handlerSelector,
      ClusterReconciliationCycle clusterReconciliationCycle,
      ClusterResourceWatcherFactory watcherFactory) {
    super();
    this.kubeClient = kubeClient;
    this.handlerSelector = handlerSelector;
    this.clusterReconciliationCycle = clusterReconciliationCycle;
    this.watcherFactory = watcherFactory;
  }

  void onStart(@Observes StartupEvent ev) {
    printArt();
    try (KubernetesClient client = kubeClient.create()) {
      LOGGER.info("Kubernetes version: {}", client.getVersion().getGitVersion());
      LOGGER.info("URL of this Kubernetes cluster: {}", client.getMasterUrl());
      if (!hasCustomResource(client, StackGresProfileDefinition.NAME)
          || !hasCustomResource(client, StackGresPostgresConfigDefinition.NAME)
          || !hasCustomResource(client, StackGresClusterDefinition.NAME)) {
        throw new RuntimeException("Some required CRDs does not exists");
      }
      registerResources();
      startClusterWatchers(client);
      scheduledExecutorService.scheduleAtFixedRate(this::reconcile, 0, 10, TimeUnit.SECONDS);
    } catch (KubernetesClientException e) {
      if (e.getCause() instanceof SocketTimeoutException) {
        LOGGER.error("Kubernetes cluster is not reachable, check your connection.");
      }
      throw e;
    }
  }

  void onStop(@Observes ShutdownEvent ev) {
    LOGGER.info("The application is stopping...");
    scheduledExecutorService.shutdown();
    watches.forEach(Unchecked.consumer(Watch::close));
  }

  private void reconcile() {
    clusterReconciliationCycle.reconcile();
  }

  private void registerResources() {
    KubernetesDeserializer.registerCustomKind(StackGresClusterDefinition.APIVERSION,
        StackGresClusterDefinition.KIND, StackGresCluster.class);

    KubernetesDeserializer.registerCustomKind(StackGresPostgresConfigDefinition.APIVERSION,
        StackGresPostgresConfigDefinition.KIND, StackGresPostgresConfig.class);

    KubernetesDeserializer.registerCustomKind(StackGresPgbouncerConfigDefinition.APIVERSION,
        StackGresPgbouncerConfigDefinition.KIND, StackGresPgbouncerConfig.class);

    KubernetesDeserializer.registerCustomKind(StackGresProfileDefinition.APIVERSION,
        StackGresProfileDefinition.KIND, StackGresProfile.class);

    KubernetesDeserializer.registerCustomKind(StackGresBackupConfigDefinition.APIVERSION,
        StackGresBackupConfigDefinition.KIND, StackGresBackupConfig.class);

    KubernetesDeserializer.registerCustomKind(StackGresBackupDefinition.APIVERSION,
        StackGresBackupDefinition.KIND, StackGresBackup.class);

    handlerSelector.registerKinds();
  }

  private void startClusterWatchers(KubernetesClient client) {
    watches.add(
        ResourceUtil.getCustomResource(client, StackGresClusterDefinition.NAME)
        .map(crd -> kubeClient.create()
            .customResources(crd,
                StackGresCluster.class,
                StackGresClusterList.class,
                StackGresClusterDoneable.class)
            .inAnyNamespace()
            .watch(watcherFactory.createWatcher(action -> clusterReconciliationCycle.reconcile())))
        .orElseThrow(() -> new IllegalStateException("Some required CRDs does not exists")));
    watches.add(
        ResourceUtil.getCustomResource(client, StackGresPostgresConfigDefinition.NAME)
        .map(crd -> kubeClient.create()
            .customResources(crd,
                StackGresPostgresConfig.class,
                StackGresPostgresConfigList.class,
                StackGresPostgresConfigDoneable.class)
            .inAnyNamespace()
            .watch(watcherFactory.createWatcher(action -> clusterReconciliationCycle.reconcile())))
        .orElseThrow(() -> new IllegalStateException("Some required CRDs does not exists")));
    watches.add(
        ResourceUtil.getCustomResource(client, StackGresPgbouncerConfigDefinition.NAME)
        .map(crd -> kubeClient.create()
            .customResources(crd,
                StackGresPgbouncerConfig.class,
                StackGresPgbouncerConfigList.class,
                StackGresPgbouncerConfigDoneable.class)
            .inAnyNamespace()
            .watch(watcherFactory.createWatcher(action -> clusterReconciliationCycle.reconcile())))
        .orElseThrow(() -> new IllegalStateException("Some required CRDs does not exists")));
    watches.add(
        ResourceUtil.getCustomResource(client, StackGresProfileDefinition.NAME)
        .map(crd -> kubeClient.create()
            .customResources(crd,
                StackGresProfile.class,
                StackGresProfileList.class,
                StackGresProfileDoneable.class)
            .inAnyNamespace()
            .watch(watcherFactory.createWatcher(action -> clusterReconciliationCycle.reconcile())))
        .orElseThrow(() -> new IllegalStateException("Some required CRDs does not exists")));
    watches.add(
        ResourceUtil.getCustomResource(client, StackGresBackupConfigDefinition.NAME)
        .map(crd -> kubeClient.create()
            .customResources(crd,
                StackGresBackupConfig.class,
                StackGresBackupConfigList.class,
                StackGresBackupConfigDoneable.class)
            .inAnyNamespace()
            .watch(watcherFactory.createWatcher(action -> clusterReconciliationCycle.reconcile())))
        .orElseThrow(() -> new IllegalStateException("Some required CRDs does not exists")));
    watches.add(
        ResourceUtil.getCustomResource(client, StackGresBackupDefinition.NAME)
        .map(crd -> kubeClient.create()
            .customResources(crd,
                StackGresBackup.class,
                StackGresBackupList.class,
                StackGresBackupDoneable.class)
            .inAnyNamespace()
            .watch(watcherFactory.createWatcher(action -> clusterReconciliationCycle.reconcile())))
        .orElseThrow(() -> new IllegalStateException("Some required CRDs does not exists")));
  }

  private boolean hasCustomResource(KubernetesClient client, String crdName) {
    if (!ResourceUtil.getCustomResource(client, crdName).isPresent()) {
      LOGGER.error("CRD not found, please create it first: {}", crdName);
      return false;
    }
    return true;
  }

  private void printArt() {
    try {
      System.out.println(Resources.toString(
          Resources.getResource(StackGresOperatorApp.class, "/META-INF/banner.txt"),
          StandardCharsets.UTF_8));
    } catch (IOException ignored) {
      // ignored, not important if we can't print the ASCII-art.
    }
  }

}
