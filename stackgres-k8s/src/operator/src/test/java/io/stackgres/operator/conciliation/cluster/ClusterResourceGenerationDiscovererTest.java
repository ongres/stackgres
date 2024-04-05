/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import static java.util.Optional.ofNullable;

import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.common.PrometheusContext;
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

  private StackGresConfig config;
  private StackGresCluster resource;
  private StackGresPostgresConfig pgConfig;
  private StackGresProfile profile;
  private Optional<StackGresObjectStorage> objectStorage;
  private Optional<StackGresPoolingConfig> pooling;
  private Optional<Secret> secret;

  @BeforeEach
  public void setup() {
    this.config = Fixtures.config().loadDefault().get();
    this.resource = Fixtures.cluster().loadDefault().withLatestPostgresVersion().get();
    this.pgConfig = Fixtures.postgresConfig().loadDefault().get();
    this.profile = Fixtures.instanceProfile().loadSizeM().get();
    this.objectStorage = ofNullable(Fixtures.objectStorage().loadDefault().get());
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
        .config(config)
        .source(resource)
        .postgresConfig(pgConfig)
        .profile(profile)
        .objectStorage(objectStorage)
        .backupSecrets(Map.of("minio", secret.get()))
        .poolingConfig(pooling)
        .prometheusContext(new PrometheusContext(false, null))
        .databaseSecret(secret)
        .currentInstances(0)
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
