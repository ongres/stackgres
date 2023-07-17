/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import java.io.IOException;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.conciliation.AbstractRequiredResourceDecoratorTest;
import io.stackgres.operator.conciliation.RequiredResourceDecorator;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;

@QuarkusTest
class DbOpsRequiredResourceDecoratorTest
    extends AbstractRequiredResourceDecoratorTest<StackGresDbOpsContext> {

  @Inject
  DbOpsRequiredResourceDecorator resourceDecorator;

  private StackGresDbOps resource;

  private StackGresCluster cluster;

  private StackGresProfile profile;

  @BeforeEach
  public void setup() {
    this.resource = Fixtures.dbOps().loadMinorVersionUpgrade().get();
    this.profile = Fixtures.instanceProfile().loadSizeXs().get();
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
  protected RequiredResourceDecorator<StackGresDbOpsContext> getResourceDecorator() {
    return this.resourceDecorator;
  }

  @Override
  protected StackGresDbOpsContext getResourceContext() throws IOException {
    StackGresDbOpsStatus status = new StackGresDbOpsStatus();
    status.setOpRetries(10);
    resource.setStatus(status);
    resource.getSpec().setMaxRetries(10);
    return ImmutableStackGresDbOpsContext.builder()
        .source(resource)
        .foundCluster(cluster)
        .foundProfile(profile)
        .build();
  }

}
