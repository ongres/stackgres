/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.script;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.KubernetesTestServerSetup;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.fixture.Fixtures;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WithKubernetesTestServer(setup = KubernetesTestServerSetup.class)
@QuarkusTest
class ScriptRequiredResourcesGeneratorTest {

  @Inject
  ScriptRequiredResourcesGenerator generator;

  private StackGresScript script;

  @BeforeEach
  void setUp() {
    script = Fixtures.script().loadDefault().get();
  }

  @Test
  void givenValidScript_shouldPass() {
    generator.getRequiredResources(script);
  }

}
