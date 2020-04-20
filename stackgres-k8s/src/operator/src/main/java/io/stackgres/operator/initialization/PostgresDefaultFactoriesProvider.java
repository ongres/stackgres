/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.io.IOException;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.common.StackGresComponents;

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
    return StackGresComponents.getOrderedPostgresVersions()
        .map(StackGresComponents::getPostgresMajorVersion)
        .map(majorVersion -> {
          try {
            DefaultPostgresFactory factory = resourceFactories.get();
            factory.setPostgresVersion(majorVersion);
            factory.init();
            return factory;
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }).collect(ImmutableList.toImmutableList());
  }

  public List<DefaultCustomResourceFactory<StackGresPostgresConfig>> getFactories() {
    return factories.stream().map(postgresConfigurationFactory -> {
      DefaultCustomResourceFactory<StackGresPostgresConfig> dcrf = postgresConfigurationFactory;
      return dcrf;
    }).collect(ImmutableList.toImmutableList());
  }

  public List<PostgresConfigurationFactory> getPostgresFactories() {
    return factories;
  }

  @Inject
  public void setResourceFactories(Instance<DefaultPostgresFactory> resourceFactories) {
    this.resourceFactories = resourceFactories;
  }
}
