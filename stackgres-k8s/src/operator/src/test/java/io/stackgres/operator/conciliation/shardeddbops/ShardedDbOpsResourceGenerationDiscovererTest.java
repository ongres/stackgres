/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops;

import java.io.IOException;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsStatus;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.AbstractRequiredResourceGeneratorTest;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.BeforeEach;

@QuarkusTest
class ShardedDbOpsResourceGenerationDiscovererTest
    extends AbstractRequiredResourceGeneratorTest<StackGresShardedDbOpsContext> {

  @Inject
  ShardedDbOpsResourceGenerationDiscoverer resourceGenerationDiscoverer;

  private StackGresShardedDbOps resource;

  private StackGresShardedCluster cluster;

  private StackGresProfile profile;

  @BeforeEach
  public void setup() {
    this.resource = Fixtures.shardedDbOps().loadRestart().get();
    this.profile = Fixtures.instanceProfile().loadSizeXs().get();
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
    return ImmutableStackGresShardedDbOpsContext.builder()
        .source(resource)
        .foundShardedCluster(cluster)
        .foundProfile(profile)
        .build();
  }

}
