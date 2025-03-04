/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.script;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.operator.conciliation.AbstractDeployedResourcesScanner;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.DeployedResourcesSnapshot;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ScriptDeployedResourceScanner
    extends AbstractDeployedResourcesScanner<StackGresScript> {

  @Inject
  public ScriptDeployedResourceScanner(DeployedResourcesCache deployedResourcesCache) {
    super(deployedResourcesCache);
  }

  public ScriptDeployedResourceScanner() {
    super(null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
  }

  @Override
  public DeployedResourcesSnapshot getDeployedResources(
      StackGresScript config, List<HasMetadata> requiredResources) {
    return DeployedResourcesSnapshot.emptySnapshot(config);
  }

  @Override
  protected Map<String, String> getGenericLabels(StackGresScript config) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected KubernetesClient getClient() {
    throw new UnsupportedOperationException();
  }

  @Override
  protected Map<Class<? extends HasMetadata>, Function<KubernetesClient,
      MixedOperation<? extends HasMetadata, ? extends KubernetesResourceList<? extends HasMetadata>,
          ? extends Resource<? extends HasMetadata>>>> getInNamepspaceResourceOperations(
              StackGresScript config) {
    throw new UnsupportedOperationException();
  }

}
