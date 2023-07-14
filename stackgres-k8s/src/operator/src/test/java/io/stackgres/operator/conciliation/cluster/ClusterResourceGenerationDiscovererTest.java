/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import static java.util.Optional.ofNullable;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.common.Prometheus;
import io.stackgres.operator.conciliation.AbstractRequiredResourceGeneratorTest;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;

@QuarkusTest
@WithKubernetesTestServer
class ClusterResourceGenerationDiscovererTest
    extends AbstractRequiredResourceGeneratorTest<StackGresClusterContext> {

  @Inject
  ClusterResourceGenerationDiscoverer resourceGenerationDiscoverer;

  private StackGresCluster resource;
  private StackGresPostgresConfig pgConfig;
  private StackGresProfile profile;
  private Optional<StackGresBackupConfig> backupConfig;
  private Optional<StackGresPoolingConfig> pooling;
  private Optional<Secret> secret;

  @BeforeEach
  public void setup() {
    this.resource = Fixtures.cluster().loadDefault().withLatestPostgresVersion().get();
    this.pgConfig = Fixtures.postgresConfig().loadDefault().get();
    this.profile = Fixtures.instanceProfile().loadSizeS().get();
    this.backupConfig = ofNullable(null);
    this.pooling = ofNullable(Fixtures.poolingConfig().loadDefault().get());
    this.secret = ofNullable(Fixtures.secret().loadMinio().get());
  }

  @Override
  protected ResourceGenerationDiscoverer<StackGresClusterContext>
      getResourceGenerationDiscoverer() {
    return this.resourceGenerationDiscoverer;
  }

  @Override
  protected StackGresClusterContext getResourceContext() {
    return ImmutableStackGresClusterContext.builder()
        .source(resource)
        .postgresConfig(pgConfig)
        .profile(profile)
        .backupConfig(backupConfig)
        .poolingConfig(pooling)
        .prometheus(new Prometheus(false, null))
        .databaseSecret(secret)
        .build();
  }

  @Override
  protected String usingKind() {
    return StackGresCluster.KIND;
  }

  @Override
  protected HasMetadata getResource() {
    return resource;
  }

}
