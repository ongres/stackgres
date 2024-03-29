/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.List;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
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
    return Seq.seq(StackGresComponent.POSTGRESQL.getLatest().streamOrderedMajorVersions())
        .<PostgresConfigurationFactory>map(majorVersion -> {
          DefaultPostgresFactory factory = resourceFactories.get();
          factory.setPostgresVersion(majorVersion);
          factory.init();
          return factory;
        })
        .toList();
  }

  public List<DefaultCustomResourceFactory<StackGresPostgresConfig>> getFactories() {
    return factories
        .stream()
        .map(factory -> (DefaultCustomResourceFactory<StackGresPostgresConfig>) factory)
        .toList();
  }

  public List<PostgresConfigurationFactory> getPostgresFactories() {
    return factories;
  }

  @Inject
  public void setResourceFactories(Instance<DefaultPostgresFactory> resourceFactories) {
    this.resourceFactories = resourceFactories;
  }
}
