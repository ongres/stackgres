/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
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
import io.stackgres.operator.sidecars.pooling.customresources.StackGresPoolingConfig;
import io.stackgres.operator.sidecars.pooling.customresources.StackGresPoolingConfigDefinition;
import io.stackgres.operator.sidecars.pooling.customresources.StackGresPoolingConfigDoneable;
import io.stackgres.operator.sidecars.pooling.customresources.StackGresPoolingConfigList;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Unchecked;

@ApplicationScoped
public class OperatorWatchersHandlerImpl implements OperatorWatcherHandler {

  private final List<Watch> watches = new ArrayList<>();

  private final KubernetesClientFactory kubeClient;

  private final ClusterReconciliationCycle clusterReconciliationCycle;
  private final ClusterResourceWatcherFactory watcherFactory;

  @Inject
  public OperatorWatchersHandlerImpl(KubernetesClientFactory kubeClient,
                                     ClusterReconciliationCycle clusterReconciliationCycle,
                                     ClusterResourceWatcherFactory watcherFactory) {
    this.kubeClient = kubeClient;
    this.clusterReconciliationCycle = clusterReconciliationCycle;
    this.watcherFactory = watcherFactory;
  }

  @Override
  public void startWatchers() {
    try (KubernetesClient client = kubeClient.create()) {

      watches.add(
          ResourceUtil.getCustomResource(client, StackGresClusterDefinition.NAME)
              .map(crd -> kubeClient.create()
                  .customResources(crd,
                      StackGresCluster.class,
                      StackGresClusterList.class,
                      StackGresClusterDoneable.class)
                  .inAnyNamespace()
                  .watch(watcherFactory.createWatcher(action -> reconcile())))
              .orElseThrow(() -> new IllegalStateException("Some required CRDs does not exists")));
      watches.add(
          ResourceUtil.getCustomResource(client, StackGresPostgresConfigDefinition.NAME)
              .map(crd -> kubeClient.create()
                  .customResources(crd,
                      StackGresPostgresConfig.class,
                      StackGresPostgresConfigList.class,
                      StackGresPostgresConfigDoneable.class)
                  .inAnyNamespace()
                  .watch(watcherFactory.createWatcher(action -> reconcile())))
              .orElseThrow(() -> new IllegalStateException("Some required CRDs does not exists")));
      watches.add(
          ResourceUtil.getCustomResource(client, StackGresPoolingConfigDefinition.NAME)
              .map(crd -> kubeClient.create()
                  .customResources(crd,
                      StackGresPoolingConfig.class,
                      StackGresPoolingConfigList.class,
                      StackGresPoolingConfigDoneable.class)
                  .inAnyNamespace()
                  .watch(watcherFactory.createWatcher(action -> reconcile())))
              .orElseThrow(() -> new IllegalStateException("Some required CRDs does not exists")));
      watches.add(
          ResourceUtil.getCustomResource(client, StackGresProfileDefinition.NAME)
              .map(crd -> kubeClient.create()
                  .customResources(crd,
                      StackGresProfile.class,
                      StackGresProfileList.class,
                      StackGresProfileDoneable.class)
                  .inAnyNamespace()
                  .watch(watcherFactory.createWatcher(action -> reconcile())))
              .orElseThrow(() -> new IllegalStateException("Some required CRDs does not exists")));
      watches.add(
          ResourceUtil.getCustomResource(client, StackGresBackupConfigDefinition.NAME)
              .map(crd -> kubeClient.create()
                  .customResources(crd,
                      StackGresBackupConfig.class,
                      StackGresBackupConfigList.class,
                      StackGresBackupConfigDoneable.class)
                  .inAnyNamespace()
                  .watch(watcherFactory.createWatcher(action -> reconcile())))
              .orElseThrow(() -> new IllegalStateException("Some required CRDs does not exists")));
      watches.add(
          ResourceUtil.getCustomResource(client, StackGresBackupDefinition.NAME)
              .map(crd -> kubeClient.create()
                  .customResources(crd,
                      StackGresBackup.class,
                      StackGresBackupList.class,
                      StackGresBackupDoneable.class)
                  .inAnyNamespace()
                  .watch(watcherFactory.createWatcher(action -> reconcile())))
              .orElseThrow(() -> new IllegalStateException("Some required CRDs does not exists")));

    }

  }

  private void reconcile() {
    clusterReconciliationCycle.reconcile();
  }

  @Override
  public void stopWatchers() {
    watches.forEach(Unchecked.consumer(Watch::close));
  }

}
