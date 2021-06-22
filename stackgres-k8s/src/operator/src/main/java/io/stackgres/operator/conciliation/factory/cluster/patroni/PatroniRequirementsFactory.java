/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import javax.inject.Singleton;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ResourceFactory;

@Singleton
public class PatroniRequirementsFactory
    implements ResourceFactory<StackGresClusterContext, ResourceRequirements> {

  @Override
  public ResourceRequirements createResource(StackGresClusterContext source) {

    var profile = source.getStackGresProfile();

    ResourceRequirements podResources = new ResourceRequirements();
    podResources.setRequests(ImmutableMap.of(
        "cpu", new Quantity(profile.getSpec().getCpu()),
        "memory", new Quantity(profile.getSpec().getMemory())));
    podResources.setLimits(ImmutableMap.of(
        "cpu", new Quantity(profile.getSpec().getCpu()),
        "memory", new Quantity(profile.getSpec().getMemory())));

    return podResources;

  }
}
