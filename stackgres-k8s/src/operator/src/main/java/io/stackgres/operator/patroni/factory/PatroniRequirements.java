/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni.factory;

import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operatorframework.resource.factory.SubResourceFactory;

@ApplicationScoped
public class PatroniRequirements
    implements SubResourceFactory<ResourceRequirements, StackGresClusterContext> {

  @Override
  public ResourceRequirements createResource(StackGresClusterContext config) {
    final Optional<StackGresProfile> profile = config.getProfile();

    ResourceRequirements podResources = new ResourceRequirements();
    if (profile.isPresent()) {
      podResources.setRequests(ImmutableMap.of(
          "cpu", new Quantity(profile.get().getSpec().getCpu()),
          "memory", new Quantity(profile.get().getSpec().getMemory())));
      podResources.setLimits(ImmutableMap.of(
          "cpu", new Quantity(profile.get().getSpec().getCpu()),
          "memory", new Quantity(profile.get().getSpec().getMemory())));
    }

    return podResources;
  }
}
