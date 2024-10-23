/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.script;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.AbstractDeployedResourcesScanner;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ScriptConciliator extends AbstractConciliator<StackGresScript> {

  @Inject
  public ScriptConciliator(
      KubernetesClient client,
      CustomResourceFinder<StackGresScript> finder,
      RequiredResourceGenerator<StackGresScript> requiredResourceGenerator,
      AbstractDeployedResourcesScanner<StackGresScript> deployedResourcesScanner,
      DeployedResourcesCache deployedResourcesCache) {
    super(client, finder, requiredResourceGenerator, deployedResourcesScanner, deployedResourcesCache);
  }

}
