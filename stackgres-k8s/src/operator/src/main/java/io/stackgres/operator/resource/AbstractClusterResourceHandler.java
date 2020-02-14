/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.function.Function;

import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.CronJob;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresUtil;
import io.stackgres.operatorframework.resource.AbstractResourceHandler;

public abstract class AbstractClusterResourceHandler
    extends AbstractResourceHandler<StackGresClusterContext> {

  protected static final ImmutableMap<Class<? extends HasMetadata>,
      Function<KubernetesClient,
      MixedOperation<? extends HasMetadata,
          ? extends KubernetesResourceList<? extends HasMetadata>, ?,
          ? extends Resource<? extends HasMetadata, ?>>>> STACKGRES_CLUSTER_RESOURCE_OPERATIONS =
      ImmutableMap.<Class<? extends HasMetadata>, Function<KubernetesClient,
          MixedOperation<? extends HasMetadata,
              ? extends KubernetesResourceList<? extends HasMetadata>, ?,
              ? extends Resource<? extends HasMetadata, ?>>>>builder()
      .put(StatefulSet.class, client -> client.apps().statefulSets())
      .put(Service.class, KubernetesClient::services)
      .put(ServiceAccount.class, KubernetesClient::serviceAccounts)
      .put(Role.class, client -> client.rbac().roles())
      .put(RoleBinding.class, client -> client.rbac().roleBindings())
      .put(Secret.class, KubernetesClient::secrets)
      .put(ConfigMap.class, KubernetesClient::configMaps)
      .put(Endpoints.class, KubernetesClient::endpoints)
      .put(CronJob.class, client -> client.batch().cronjobs())
      .put(Pod.class, client -> client.pods())
      .put(PersistentVolumeClaim.class, client -> client.persistentVolumeClaims())
      .put(Job.class, client -> client.batch().jobs())
      .build();

  @Override
  protected <M extends HasMetadata> Function<KubernetesClient,
      MixedOperation<? extends HasMetadata, ? extends KubernetesResourceList<? extends HasMetadata>,
          ?, ? extends Resource<? extends HasMetadata, ?>>> getResourceOperations(M resource) {
    return STACKGRES_CLUSTER_RESOURCE_OPERATIONS.get(resource.getClass());
  }

  @Override
  public String getContextNamespaceOf(HasMetadata resource) {
    return resource.getMetadata().getNamespace();
  }

  @Override
  public String getContextNameOf(HasMetadata resource) {
    return resource.getMetadata().getLabels().get(StackGresUtil.CLUSTER_NAME_KEY);
  }

}
