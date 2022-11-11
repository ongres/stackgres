/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.script;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.common.StackGresKubernetesClient;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.operator.conciliation.DeployedResourcesScanner;

@ApplicationScoped
public class ScriptDeployedResourceScanner extends DeployedResourcesScanner<StackGresScript> {

  @Override
  public List<HasMetadata> getDeployedResources(StackGresScript config) {
    return List.of();
  }

  @Override
  protected Map<String, String> getGenericLabels(StackGresScript config) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected StackGresKubernetesClient getClient() {
    throw new UnsupportedOperationException();
  }

  @Override
  protected Map<Class<? extends HasMetadata>, Function<KubernetesClient,
      MixedOperation<? extends HasMetadata, ? extends KubernetesResourceList<? extends HasMetadata>,
          ? extends Resource<? extends HasMetadata>>>> getInNamepspaceResourceOperations() {
    throw new UnsupportedOperationException();
  }

}
