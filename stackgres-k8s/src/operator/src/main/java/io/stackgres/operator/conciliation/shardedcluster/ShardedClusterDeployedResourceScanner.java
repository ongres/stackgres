/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import java.util.Map;
import java.util.function.Function;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.external.shardingsphere.ComputeNode;
import io.stackgres.common.crd.external.shardingsphere.ComputeNodeList;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigList;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigList;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileList;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptList;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.operator.conciliation.AbstractDeployedResourcesScanner;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.ReconciliationOperations;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ShardedClusterDeployedResourceScanner
    extends AbstractDeployedResourcesScanner<StackGresShardedCluster>
    implements ReconciliationOperations {

  private final KubernetesClient client;
  private final LabelFactoryForShardedCluster labelFactory;

  @Inject
  public ShardedClusterDeployedResourceScanner(
      DeployedResourcesCache deployedResourcesCache,
      KubernetesClient client,
      LabelFactoryForShardedCluster labelFactory) {
    super(deployedResourcesCache);
    this.client = client;
    this.labelFactory = labelFactory;
  }

  public ShardedClusterDeployedResourceScanner() {
    super(null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.client = null;
    this.labelFactory = null;
  }

  @Override
  protected Map<String, String> getGenericLabels(StackGresShardedCluster config) {
    return labelFactory.genericLabels(config);
  }

  @Override
  protected KubernetesClient getClient() {
    return client;
  }

  @Override
  protected Map<Class<? extends HasMetadata>,
      Function<KubernetesClient, MixedOperation<? extends HasMetadata,
          ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>> getInNamepspaceResourceOperations(
                  StackGresShardedCluster config) {
    return IN_NAMESPACE_RESOURCE_OPERATIONS;
  }

  static final Map<
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
          Map.entry(Endpoints.class, KubernetesClient::endpoints),
          Map.entry(Service.class, KubernetesClient::services),
          Map.entry(ServiceAccount.class, KubernetesClient::serviceAccounts),
          Map.entry(Role.class, client -> client.rbac().roles()),
          Map.entry(RoleBinding.class, client -> client.rbac().roleBindings()),
          Map.entry(CronJob.class, client -> client.batch().v1().cronjobs()),
          Map.entry(StackGresProfile.class, client -> client
              .resources(StackGresProfile.class, StackGresProfileList.class)),
          Map.entry(StackGresPostgresConfig.class, client -> client
              .resources(StackGresPostgresConfig.class, StackGresPostgresConfigList.class)),
          Map.entry(StackGresPoolingConfig.class, client -> client
              .resources(StackGresPoolingConfig.class, StackGresPoolingConfigList.class)),
          Map.entry(StackGresScript.class, client -> client
              .resources(StackGresScript.class, StackGresScriptList.class)),
          Map.entry(StackGresCluster.class, client -> client
              .resources(StackGresCluster.class, StackGresClusterList.class)),
          Map.entry(ComputeNode.class, client -> client
              .resources(ComputeNode.class, ComputeNodeList.class))
          );

}
