/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config;

import java.util.List;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.VersionInfo;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ConfigRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresConfig> {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(ConfigRequiredResourcesGenerator.class);

  private final Supplier<VersionInfo> kubernetesVersionSupplier;

  private final ResourceGenerationDiscoverer<StackGresConfigContext> discoverer;

  @Inject
  public ConfigRequiredResourcesGenerator(
      Supplier<VersionInfo> kubernetesVersionSupplier,
      ResourceGenerationDiscoverer<StackGresConfigContext> discoverer) {
    this.kubernetesVersionSupplier = kubernetesVersionSupplier;
    this.discoverer = discoverer;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresConfig config) {
    VersionInfo kubernetesVersion = kubernetesVersionSupplier.get();

    StackGresConfigContext context = ImmutableStackGresConfigContext.builder()
        .kubernetesVersion(kubernetesVersion)
        .source(config)
        .build();

    return discoverer.generateResources(context);
  }

}
