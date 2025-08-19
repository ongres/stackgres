/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgconfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBuilder;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.common.StackGresPostgresConfigReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractValuesMutator;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PgConfigDefaultValuesMutator
    extends AbstractValuesMutator<StackGresPostgresConfig, StackGresPostgresConfigReview, StackGresCluster>
    implements PgConfigMutator {

  private final PgConfigNormalizeValuesMutator normalizeValuesMutator;

  public PgConfigDefaultValuesMutator(
      DefaultCustomResourceFactory<StackGresPostgresConfig, StackGresCluster> factory,
      ObjectMapper jsonMapper) {
    super(factory, jsonMapper);
    this.normalizeValuesMutator = new PgConfigNormalizeValuesMutator();
  }

  @Override
  protected StackGresCluster createSourceResource(StackGresPostgresConfig resource) {
    return new StackGresClusterBuilder()
        .withNewMetadata()
        .withName(resource.getMetadata().getName())
        .endMetadata()
        .withNewSpec()
        .withNewPostgres()
        .withVersion(resource.getSpec().getPostgresVersion())
        .endPostgres()
        .endSpec()
        .withNewStatus()
        .withPostgresVersion(resource.getSpec().getPostgresVersion())
        .endStatus()
        .build();
  }

  public StackGresPostgresConfig mutate(StackGresPostgresConfigReview review, StackGresPostgresConfig resource) {
    resource = super.mutate(review, resource);
    resource = normalizeValuesMutator.mutate(review, resource);
    return resource;
  }

  @Override
  protected Class<StackGresPostgresConfig> getResourceClass() {
    return StackGresPostgresConfig.class;
  }

}
