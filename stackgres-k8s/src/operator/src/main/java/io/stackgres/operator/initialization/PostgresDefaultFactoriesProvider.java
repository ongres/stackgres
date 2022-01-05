/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class PostgresDefaultFactoriesProvider
    implements DefaultFactoryProvider<DefaultCustomResourceFactory<StackGresPostgresConfig>> {

  private Instance<DefaultPostgresFactory> resourceFactories;

  private List<PostgresConfigurationFactory> factories;

  @PostConstruct
  public void init() {
    factories = buildFactories();
  }

  public List<PostgresConfigurationFactory> buildFactories() {
    return Seq.seq(StackGresComponent.POSTGRESQL.getLatest().getOrderedMajorVersions())
        .map(majorVersion -> {
          DefaultPostgresFactory factory = resourceFactories.get();
          factory.setPostgresVersion(majorVersion);
          factory.init();
          return factory;
        }).collect(ImmutableList.toImmutableList());
  }

  public List<DefaultCustomResourceFactory<StackGresPostgresConfig>> getFactories() {
    return factories.stream()
        .collect(ImmutableList.toImmutableList());
  }

  public List<PostgresConfigurationFactory> getPostgresFactories() {
    return factories;
  }

  @Inject
  public void setResourceFactories(Instance<DefaultPostgresFactory> resourceFactories) {
    this.resourceFactories = resourceFactories;
  }
}
