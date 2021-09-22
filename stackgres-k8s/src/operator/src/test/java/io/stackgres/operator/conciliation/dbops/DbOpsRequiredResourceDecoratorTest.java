/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import java.io.IOException;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.conciliation.AbstractRequiredResourceDecoratorTest;
import io.stackgres.operator.conciliation.RequiredResourceDecorator;
import io.stackgres.operator.fixture.StackGresClusterFixture;
import io.stackgres.operator.fixture.StackGresDbOpsFixture;
import org.junit.jupiter.api.BeforeEach;

@QuarkusTest
class DbOpsRequiredResourceDecoratorTest
    extends AbstractRequiredResourceDecoratorTest<StackGresDbOpsContext> {

  @Inject
  DbOpsRequiredResourceDecorator resourceDecorator;

  private StackGresDbOps resource;

  private StackGresCluster cluster;

  @BeforeEach
  public void setup() {
    this.resource = new StackGresDbOpsFixture().build("minor_version_upgrade");
    this.cluster = new StackGresClusterFixture().build("default");
  }

  @Override
  protected String usingCrdFilename() {
    return "SGDbOps.yaml";
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
        .cluster(cluster)
        .build();
  }

  @Override
  protected void injectExtraLabelsGeneratedByKubernetes(HasMetadata resource) {
    if (resource instanceof Job) {
      PodTemplateSpec template = ((Job) resource).getSpec().getTemplate();
      resource.getMetadata().getLabels().put("job-name", template.getMetadata().getName());
    }
  }

}
