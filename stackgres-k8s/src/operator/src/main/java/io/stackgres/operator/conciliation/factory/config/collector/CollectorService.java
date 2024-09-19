/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config.collector;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigCollector;
import io.stackgres.common.crd.sgconfig.StackGresConfigCollectorService;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeploy;
import io.stackgres.common.crd.sgconfig.StackGresConfigService;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.labels.LabelFactoryForConfig;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class CollectorService
    implements ResourceGenerator<StackGresConfigContext> {

  private final LabelFactoryForConfig labelFactory;

  @Inject
  public CollectorService(LabelFactoryForConfig labelFactory) {
    this.labelFactory = labelFactory;
  }

  /**
   * Create the Secret for Web Console.
   */
  @Override
  public @NotNull Stream<HasMetadata> generateResource(StackGresConfigContext context) {
    if (!Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresConfigSpec::getDeploy)
        .map(StackGresConfigDeploy::getCollector)
        .orElse(true)
        || context.getObservedClusters().isEmpty()) {
      return Stream.of();
    }

    final StackGresConfig config = context.getSource();
    final String namespace = config.getMetadata().getNamespace();
    final Map<String, String> labels = labelFactory.genericLabels(config);

    return Stream.of(new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(CollectorDeployment.name(config))
        .withLabels(labels)
        .withAnnotations(Optional.of(config.getSpec())
            .map(StackGresConfigSpec::getCollector)
            .map(StackGresConfigCollector::getService)
            .map(StackGresConfigService::getAnnotations)
            .orElse(null))
        .endMetadata()
        .withSpec(Optional.of(config.getSpec())
            .map(StackGresConfigSpec::getCollector)
            .map(StackGresConfigCollector::getService)
            .map(StackGresConfigCollectorService::getSpec)
            .orElseGet(ServiceSpec::new))
        .editSpec()
        .withSelector(labelFactory.collectorLabels(config))
        .endSpec()
        .build());
  }

}
