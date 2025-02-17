/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.stream;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StreamSourceType;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StreamRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresStream> {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(StreamRequiredResourcesGenerator.class);

  private final CustomResourceScanner<StackGresConfig> configScanner;

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  private final ResourceGenerationDiscoverer<StackGresStreamContext> discoverer;

  @Inject
  public StreamRequiredResourcesGenerator(
      CustomResourceScanner<StackGresConfig> configScanner,
      CustomResourceFinder<StackGresCluster> clusterFinder,
      ResourceGenerationDiscoverer<StackGresStreamContext> discoverer) {
    this.configScanner = configScanner;
    this.clusterFinder = clusterFinder;
    this.discoverer = discoverer;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresStream stream) {
    final StackGresConfig config = configScanner.findResources()
        .stream()
        .filter(list -> list.size() == 1)
        .flatMap(List::stream)
        .findAny()
        .orElseThrow(() -> new IllegalArgumentException(
            "SGConfig not found or more than one exists. Aborting reoconciliation!"));

    if (Objects.equals(
            stream.getSpec().getSource().getType(),
            StreamSourceType.SGCLUSTER.toString())) {
      String clusterName = stream.getSpec().getSource().getSgCluster().getName();
      String streamNamespace = stream.getMetadata().getNamespace();
      Optional<StackGresCluster> foundCluster = clusterFinder
          .findByNameAndNamespace(clusterName, streamNamespace);
      if (foundCluster.isEmpty()) {
        throw new IllegalArgumentException(
            StackGresCluster.KIND + " " + clusterName + " not found");
      }
    }

    StackGresStreamContext context = ImmutableStackGresStreamContext.builder()
        .config(config)
        .source(stream)
        .build();

    return discoverer.generateResources(context);
  }

}
