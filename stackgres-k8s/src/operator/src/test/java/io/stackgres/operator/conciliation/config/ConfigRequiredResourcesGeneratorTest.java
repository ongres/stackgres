/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.KubernetesTestServerSetup;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.fixture.Fixtures;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WithKubernetesTestServer(setup = KubernetesTestServerSetup.class)
@QuarkusTest
class ConfigRequiredResourcesGeneratorTest {

  @Inject
  ConfigRequiredResourcesGenerator generator;

  private StackGresConfig config;

  @BeforeEach
  void setUp() {
    config = Fixtures.config().loadDefault().get();
  }

  @Test
  void givenValidConfig_shouldPass() {
    generator.getRequiredResources(config);
  }

}
