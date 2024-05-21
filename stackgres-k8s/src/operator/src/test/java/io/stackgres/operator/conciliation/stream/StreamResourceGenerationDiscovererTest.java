/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.stream;

import java.io.IOException;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamStatus;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.AbstractRequiredResourceGeneratorTest;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;

@QuarkusTest
class StreamResourceGenerationDiscovererTest
    extends AbstractRequiredResourceGeneratorTest<StackGresStreamContext> {

  @Inject
  StreamResourceGenerationDiscoverer resourceGenerationDiscoverer;

  private StackGresConfig config;

  private StackGresStream resource;

  @BeforeEach
  public void setup() {
    this.config = Fixtures.config().loadDefault().get();
    this.resource = Fixtures.stream().loadSgClusterToCloudEvent().get();
  }

  @Override
  protected String usingKind() {
    return StackGresStream.KIND;
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
  protected ResourceGenerationDiscoverer<StackGresStreamContext>
      getResourceGenerationDiscoverer() {
    return this.resourceGenerationDiscoverer;
  }

  @Override
  protected StackGresStreamContext getResourceContext() throws IOException {
    StackGresStreamStatus status = new StackGresStreamStatus();
    resource.setStatus(status);
    resource.getSpec().setMaxRetries(10);
    return ImmutableStackGresStreamContext.builder()
        .config(config)
        .source(resource)
        .build();
  }

}
