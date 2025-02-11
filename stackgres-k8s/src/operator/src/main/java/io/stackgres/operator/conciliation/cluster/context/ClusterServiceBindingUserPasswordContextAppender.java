/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterServiceBinding;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ClusterServiceBindingUserPasswordContextAppender
    extends ClusterContextAppenderWithSecrets {

  public ClusterServiceBindingUserPasswordContextAppender(
      ResourceFinder<Secret> secretFinder) {
    super(secretFinder);
  }

  @Override
  public void appendContext(StackGresCluster cluster, Builder contextBuilder) {
    final var serviceBindingConfig = Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getBinding);
    final var userPasswordForBinding = getSecretAndKeyOrThrow(
        cluster.getMetadata().getNamespace(),
        serviceBindingConfig,
        StackGresClusterServiceBinding::getPassword,
        secretKeySelector -> "Service Binding password key " + secretKeySelector.getKey()
        + " was not found in secret " + secretKeySelector.getName(),
        secretKeySelector -> "Service Binding password secret " + secretKeySelector.getName()
        + " was not found");
    contextBuilder
        .userPasswordForBinding(userPasswordForBinding);
  }

}
