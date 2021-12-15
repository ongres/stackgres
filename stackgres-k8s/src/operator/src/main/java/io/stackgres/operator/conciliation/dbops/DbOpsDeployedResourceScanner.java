/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import java.util.Map;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
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
import io.stackgres.common.LabelFactoryForDbOps;
import io.stackgres.common.StackGresKubernetesClient;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.operator.conciliation.DeployedResourcesScanner;
import io.stackgres.operator.conciliation.ReconciliationOperations;

@ApplicationScoped
public class DbOpsDeployedResourceScanner extends DeployedResourcesScanner<StackGresDbOps>
    implements ReconciliationOperations {

  private final KubernetesClient client;
  private final LabelFactoryForDbOps labelFactory;

  @Inject
  public DbOpsDeployedResourceScanner(
      KubernetesClient client,
      LabelFactoryForDbOps labelFactory) {
    this.client = client;
    this.labelFactory = labelFactory;
  }

  public DbOpsDeployedResourceScanner() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
    this.client = null;
    this.labelFactory = null;
  }

  protected Map<String, String> getGenericLabels(StackGresDbOps config) {
    return labelFactory.genericLabels(config);
  }

  protected StackGresKubernetesClient getClient() {
    return (StackGresKubernetesClient) client;
  }

  protected ImmutableMap<Class<? extends HasMetadata>,
  Function<KubernetesClient, MixedOperation<? extends HasMetadata,
      ? extends KubernetesResourceList<? extends HasMetadata>,
          ? extends Resource<? extends HasMetadata>>>> getInNamepspaceResourceOperations() {
    return IN_NAMESPACE_RESOURCE_OPERATIONS;
  }

  protected ImmutableMap<Class<? extends HasMetadata>,
      Function<KubernetesClient, MixedOperation<? extends HasMetadata,
          ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>> getAnyNamespaceResourceOperations() {
    return ImmutableMap.of();
  }

  static final ImmutableMap<
      Class<? extends HasMetadata>,
      Function<
          KubernetesClient,
          MixedOperation<
              ? extends HasMetadata,
              ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>>
      IN_NAMESPACE_RESOURCE_OPERATIONS =
      ImmutableMap.<Class<? extends HasMetadata>, Function<KubernetesClient,
          MixedOperation<? extends HasMetadata,
              ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>>builder()
          .put(ServiceAccount.class, KubernetesClient::serviceAccounts)
          .put(Role.class, client -> client.rbac().roles())
          .put(RoleBinding.class, client -> client.rbac().roleBindings())
          .put(Job.class, client -> client.batch().v1().jobs())
          .build();

}
