/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileHugePages;
import io.stackgres.common.crd.sgprofile.StackGresProfileSpec;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ResourceFactory;

@Singleton
public class PatroniRequirementsFactory
    implements ResourceFactory<StackGresClusterContext, ResourceRequirements> {

  @Override
  public ResourceRequirements createResource(StackGresClusterContext source) {
    if (Optional.of(source.getSource().getSpec())
        .map(StackGresClusterSpec::getNonProductionOptions)
        .map(StackGresClusterNonProduction::getDisablePatroniResourceRequirements)
        .orElse(false)) {
      return null;
    }

    final var profile = source.getProfile();

    final ResourceRequirements podResources = new ResourceRequirements();
    final var requests = new HashMap<String, Quantity>();
    final var limits = new HashMap<String, Quantity>();
    final Quantity cpu = new Quantity(profile.getSpec().getCpu());
    final Quantity memory = new Quantity(profile.getSpec().getMemory());
    requests.put("cpu", cpu);
    requests.put("memory", memory);
    limits.put("cpu", cpu);
    limits.put("memory", memory);
    setHugePages1Gi(profile, requests, limits);
    setHugePages2Mi(profile, requests, limits);
    podResources.setRequests(Map.copyOf(requests));
    podResources.setLimits(Map.copyOf(limits));

    return podResources;

  }

  private void setHugePages2Mi(StackGresProfile profile,
      final HashMap<String, Quantity> requests, final HashMap<String, Quantity> limits) {
    Optional.of(profile.getSpec())
        .map(StackGresProfileSpec::getHugePages)
        .map(StackGresProfileHugePages::getHugepages2Mi)
        .map(Quantity::new)
        .ifPresent(quantity -> {
          requests.put("hugepages-2Mi", quantity);
          limits.put("hugepages-2Mi", quantity);
        });
  }

  private void setHugePages1Gi(StackGresProfile profile,
      final HashMap<String, Quantity> requests, final HashMap<String, Quantity> limits) {
    Optional.of(profile.getSpec())
        .map(StackGresProfileSpec::getHugePages)
        .map(StackGresProfileHugePages::getHugepages1Gi)
        .map(Quantity::new)
        .ifPresent(quantity -> {
          requests.put("hugepages-1Gi", quantity);
          limits.put("hugepages-1Gi", quantity);
        });
  }

}
