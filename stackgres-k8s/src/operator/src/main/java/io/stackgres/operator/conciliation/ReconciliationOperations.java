/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupList;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptList;
import io.stackgres.common.prometheus.PodMonitor;
import io.stackgres.common.prometheus.PodMonitorList;

public interface ReconciliationOperations {

  List<Class<? extends HasMetadata>> RESOURCES_ORDER = List.of(
      Secret.class,
      ConfigMap.class,
      ServiceAccount.class,
      Role.class,
      RoleBinding.class,
      Endpoints.class,
      Service.class,
      PodMonitor.class,
      Pod.class,
      Job.class,
      CronJob.class,
      StatefulSet.class,
      StackGresScript.class,
      StackGresPostgresConfig.class,
      StackGresCluster.class
      );

  Comparator<HasMetadata> RESOURCES_COMPARATOR = Comparator.comparingInt(
      resource -> Optional.of(RESOURCES_ORDER.indexOf(resource.getClass()))
          .filter(index -> index > -1)
          .orElseGet(RESOURCES_ORDER::size));

  Map<
      Class<? extends HasMetadata>,
      Function<
          KubernetesClient,
          MixedOperation<
              ? extends HasMetadata,
              ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>>
      IN_NAMESPACE_RESOURCE_OPERATIONS =
      Map.<Class<? extends HasMetadata>, Function<KubernetesClient,
          MixedOperation<? extends HasMetadata,
              ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>>ofEntries(
          Map.entry(Secret.class, KubernetesClient::secrets),
          Map.entry(ConfigMap.class, KubernetesClient::configMaps),
          Map.entry(ServiceAccount.class, KubernetesClient::serviceAccounts),
          Map.entry(Role.class, client -> client.rbac().roles()),
          Map.entry(RoleBinding.class, client -> client.rbac().roleBindings()),
          Map.entry(Endpoints.class, KubernetesClient::endpoints),
          Map.entry(Service.class, KubernetesClient::services),
          Map.entry(Pod.class, client -> client.pods()),
          Map.entry(Job.class, client -> client.batch().v1().jobs()),
          Map.entry(CronJob.class, client -> client.batch().v1().cronjobs()),
          Map.entry(StatefulSet.class, client -> client.apps().statefulSets()),
          Map.entry(StackGresScript.class, client -> client
              .resources(StackGresScript.class, StackGresScriptList.class)),
          Map.entry(StackGresBackup.class, client -> client
              .resources(StackGresBackup.class, StackGresBackupList.class))
          );

  Map<
      Class<? extends HasMetadata>,
      Function<
          KubernetesClient,
          MixedOperation<
              ? extends HasMetadata,
              ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>>
      PROMETHEUS_RESOURCE_OPERATIONS =
      Map.<Class<? extends HasMetadata>, Function<KubernetesClient,
          MixedOperation<? extends HasMetadata,
              ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>>ofEntries(
          Map.entry(PodMonitor.class, client -> client
              .resources(PodMonitor.class, PodMonitorList.class))
          );

}
