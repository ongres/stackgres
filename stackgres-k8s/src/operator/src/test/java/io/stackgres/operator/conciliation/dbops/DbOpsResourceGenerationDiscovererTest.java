/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import java.io.IOException;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.AbstractRequiredResourceGeneratorTest;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;

@QuarkusTest
class DbOpsResourceGenerationDiscovererTest
    extends AbstractRequiredResourceGeneratorTest<StackGresDbOpsContext> {

  @Inject
  DbOpsResourceGenerationDiscoverer resourceGenerationDiscoverer;

  private StackGresConfig config;

  private StackGresDbOps resource;

  private StackGresCluster cluster;

  private StackGresProfile profile;

  @BeforeEach
  public void setup() {
    this.config = Fixtures.config().loadDefault().get();
    this.resource = Fixtures.dbOps().loadMinorVersionUpgrade().get();
    this.profile = Fixtures.instanceProfile().loadSizeS().get();
    this.cluster = Fixtures.cluster().loadDefault().withLatestPostgresVersion().get();
  }

  @Override
  protected String usingKind() {
    return StackGresDbOps.KIND;
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
  protected ResourceGenerationDiscoverer<StackGresDbOpsContext>
      getResourceGenerationDiscoverer() {
    return this.resourceGenerationDiscoverer;
  }

  @Override
  protected StackGresDbOpsContext getResourceContext() throws IOException {
    StackGresDbOpsStatus status = new StackGresDbOpsStatus();
    status.setOpRetries(10);
    resource.setStatus(status);
    resource.getSpec().setMaxRetries(10);
    return StackGresDbOpsContext.builder()
        .config(config)
        .source(resource)
        .foundCluster(cluster)
        .foundProfile(profile)
        .build();
  }

}
