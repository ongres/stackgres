/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.AbstractRequiredResourceGeneratorTest;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;

@QuarkusTest
class DistributedLogsResourceGenerationDiscovererTest
    extends AbstractRequiredResourceGeneratorTest<StackGresDistributedLogsContext> {

  @Inject
  DistributedLogsResourceGenerationDiscoverer resourceGenerationDiscoverer;

  private StackGresConfig config;

  private StackGresDistributedLogs resource;

  private StackGresPostgresConfig postgresConfig;

  private StackGresProfile profile;

  private Optional<Secret> secret;

  private StackGresCluster connectecCluster;

  @BeforeEach
  public void setup() {
    this.config = Fixtures.config().loadDefault().get();
    this.resource = Fixtures.distributedLogs().loadDefault().get();
    this.postgresConfig = Fixtures.postgresConfig().loadDefault().get();
    this.connectecCluster = Fixtures.cluster().loadDefault()
        .withLatestPostgresVersion().get();
    this.profile = Fixtures.instanceProfile().loadSizeM().get();
    this.postgresConfig = Fixtures.postgresConfig().loadDefault().get();
    this.secret = ofNullable(Fixtures.secret().loadMinio().get());
  }

  @Override
  protected String usingKind() {
    return StackGresDistributedLogs.KIND;
  }

  @Override
  protected HasMetadata getResource() {
    return this.resource;
  }

  @Override
  protected ResourceGenerationDiscoverer<StackGresDistributedLogsContext>
      getResourceGenerationDiscoverer() {
    return resourceGenerationDiscoverer;
  }

  @Override
  protected StackGresDistributedLogsContext getResourceContext() throws IOException {
    return ImmutableStackGresDistributedLogsContext.builder()
        .config(config)
        .source(resource)
        .postgresConfig(postgresConfig)
        .profile(profile)
        .addAllConnectedClusters(List.of(connectecCluster))
        .databaseCredentials(secret)
        .build();
  }

}
