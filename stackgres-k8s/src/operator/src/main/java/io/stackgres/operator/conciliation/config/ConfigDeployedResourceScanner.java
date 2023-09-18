/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config;

import java.util.Map;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.labels.LabelFactoryForConfig;
import io.stackgres.operator.conciliation.AbstractDeployedResourcesScanner;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.ReconciliationOperations;

@ApplicationScoped
public class ConfigDeployedResourceScanner
    extends AbstractDeployedResourcesScanner<StackGresConfig>
    implements ReconciliationOperations {

  private final KubernetesClient client;
  private final LabelFactoryForConfig labelFactory;

  @Inject
  public ConfigDeployedResourceScanner(
      DeployedResourcesCache deployedResourcesCache,
      KubernetesClient client,
      LabelFactoryForConfig labelFactory) {
    super(deployedResourcesCache);
    this.client = client;
    this.labelFactory = labelFactory;
  }

  public ConfigDeployedResourceScanner() {
    super(null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.client = null;
    this.labelFactory = null;
  }

  @Override
  protected Map<String, String> getGenericLabels(StackGresConfig config) {
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
          Map.entry(Secret.class, KubernetesClient::secrets),
          Map.entry(ConfigMap.class, KubernetesClient::configMaps),
          Map.entry(ServiceAccount.class, KubernetesClient::serviceAccounts),
          Map.entry(Service.class, KubernetesClient::services),
          Map.entry(Role.class, client -> client.rbac().roles()),
          Map.entry(RoleBinding.class, client -> client.rbac().roleBindings()),
          Map.entry(Job.class, client -> client.batch().v1().jobs()),
          Map.entry(Deployment.class, client -> client.apps().deployments()));

}
