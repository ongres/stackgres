/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config.collector;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigCollector;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeploy;
import io.stackgres.common.crd.sgconfig.StackGresConfigServiceAccount;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.labels.LabelFactoryForConfig;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder
public class CollectorServiceAccount
    implements ResourceGenerator<StackGresConfigContext> {

  private final LabelFactoryForConfig labelFactory;

  @Inject
  public CollectorServiceAccount(LabelFactoryForConfig labelFactory) {
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

    return Stream.of(new ServiceAccountBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(CollectorDeployments.name(config))
        .withLabels(labels)
        .withAnnotations(Optional.of(config.getSpec())
            .map(StackGresConfigSpec::getServiceAccount)
            .map(StackGresConfigServiceAccount::getAnnotations)
            .orElse(null))
        .endMetadata()
        .withImagePullSecrets(Seq.seq(Optional.of(config.getSpec())
            .map(StackGresConfigSpec::getCollector)
            .map(StackGresConfigCollector::getServiceAccount)
            .map(StackGresConfigServiceAccount::getRepoCredentials)
            .map(repoCredentials -> repoCredentials.stream()
                .map(LocalObjectReference::new)
                .toList()))
            .append(Optional.ofNullable(context.getSource().getSpec().getImagePullSecrets())
                .stream()
                .flatMap(List::stream)
                .map(LocalObjectReference.class::cast)
                .toList())
            .flatMap(List::stream)
            .toList())
        .build());
  }

}
