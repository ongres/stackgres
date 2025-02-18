/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops;

import java.io.IOException;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsStatus;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.AbstractRequiredResourceGeneratorTest;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;

@QuarkusTest
class ShardedDbOpsResourceGenerationDiscovererTest
    extends AbstractRequiredResourceGeneratorTest<StackGresShardedDbOpsContext> {

  @Inject
  ShardedDbOpsResourceGenerationDiscoverer resourceGenerationDiscoverer;

  private StackGresConfig config;

  private StackGresShardedDbOps resource;

  private StackGresShardedCluster cluster;

  private StackGresProfile profile;

  @BeforeEach
  public void setup() {
    this.config = Fixtures.config().loadDefault().get();
    this.resource = Fixtures.shardedDbOps().loadRestart().get();
    this.profile = Fixtures.instanceProfile().loadSizeS().get();
    this.cluster = Fixtures.shardedCluster().loadDefault().withLatestPostgresVersion().get();
  }

  @Override
  protected String usingKind() {
    return StackGresShardedDbOps.KIND;
  }

  @Override
  protected HasMetadata getResource() {
    return this.resource;
  }

  @Override
  public void assertThatResourceNameIsComplaint(HasMetadata resource) {
    ResourceUtil.nameIsValidService(resource.getMetadata().getName());
  }

  @Override
  protected ResourceGenerationDiscoverer<StackGresShardedDbOpsContext>
      getResourceGenerationDiscoverer() {
    return this.resourceGenerationDiscoverer;
  }

  @Override
  protected StackGresShardedDbOpsContext getResourceContext() throws IOException {
    StackGresShardedDbOpsStatus status = new StackGresShardedDbOpsStatus();
    status.setOpRetries(10);
    resource.setStatus(status);
    resource.getSpec().setMaxRetries(10);
    return StackGresShardedDbOpsContext.builder()
        .config(config)
        .source(resource)
        .foundShardedCluster(cluster)
        .foundProfile(profile)
        .build();
  }

}
