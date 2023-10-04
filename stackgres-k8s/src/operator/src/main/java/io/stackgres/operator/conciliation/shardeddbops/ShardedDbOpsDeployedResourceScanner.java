/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops;

import java.util.Map;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.labels.LabelFactoryForShardedDbOps;
import io.stackgres.operator.conciliation.AbstractDeployedResourcesScanner;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.ReconciliationOperations;

@ApplicationScoped
public class ShardedDbOpsDeployedResourceScanner
    extends AbstractDeployedResourcesScanner<StackGresShardedDbOps>
    implements ReconciliationOperations {

  private final KubernetesClient client;
  private final LabelFactoryForShardedDbOps labelFactory;

  @Inject
  public ShardedDbOpsDeployedResourceScanner(
      DeployedResourcesCache deployedResourcesCache,
      KubernetesClient client,
      LabelFactoryForShardedDbOps labelFactory) {
    super(deployedResourcesCache);
    this.client = client;
    this.labelFactory = labelFactory;
  }

  public ShardedDbOpsDeployedResourceScanner() {
    super(null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.client = null;
    this.labelFactory = null;
  }

  @Override
  protected Map<String, String> getGenericLabels(StackGresShardedDbOps config) {
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
              ? extends Resource<? extends HasMetadata>>>> getInNamepspaceResourceOperations() {
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
          Map.entry(ServiceAccount.class, KubernetesClient::serviceAccounts),
          Map.entry(Role.class, client -> client.rbac().roles()),
          Map.entry(RoleBinding.class, client -> client.rbac().roleBindings()),
          Map.entry(Job.class, client -> client.batch().v1().jobs())
          );

}
