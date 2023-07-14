/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileHugePages;
import io.stackgres.common.crd.sgprofile.StackGresProfileRequests;
import io.stackgres.common.crd.sgprofile.StackGresProfileSpec;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import jakarta.inject.Singleton;

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
    final Quantity cpuLimit = new Quantity(profile.getSpec().getCpu());
    final Quantity memoryLimit = new Quantity(profile.getSpec().getMemory());
    final var limits = new HashMap<String, Quantity>();
    limits.put("cpu", cpuLimit);
    limits.put("memory", memoryLimit);
    final Quantity cpuRequest = Optional.of(profile.getSpec())
        .map(StackGresProfileSpec::getRequests)
        .map(StackGresProfileRequests::getCpu)
        .map(Quantity::new)
        .filter(q -> Optional.ofNullable(source.getSource().getSpec().getNonProductionOptions())
            .map(StackGresClusterNonProduction::getEnableSetPatroniCpuRequests)
            .orElse(false))
        .orElse(cpuLimit);
    final Quantity memoryRequest = Optional.of(profile.getSpec())
        .map(StackGresProfileSpec::getRequests)
        .map(StackGresProfileRequests::getMemory)
        .map(Quantity::new)
        .filter(q -> Optional.ofNullable(source.getSource().getSpec().getNonProductionOptions())
            .map(StackGresClusterNonProduction::getEnableSetPatroniMemoryRequests)
            .orElse(false))
        .orElse(memoryLimit);
    final var requests = new HashMap<String, Quantity>();
    requests.put("cpu", cpuRequest);
    requests.put("memory", memoryRequest);
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
