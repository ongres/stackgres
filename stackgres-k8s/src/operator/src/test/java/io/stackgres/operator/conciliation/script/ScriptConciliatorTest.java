/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.script;

import static org.mockito.Mockito.when;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.Conciliator;
import io.stackgres.operator.conciliation.ConciliatorTest;
import io.stackgres.operator.conciliation.DeployedResourcesScanner;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScriptConciliatorTest extends ConciliatorTest<StackGresScript> {

  private final StackGresScript script = Fixtures.script().loadDefault().get();

  @Mock
  private RequiredResourceGenerator<StackGresScript> requiredResourceGenerator;

  @Mock
  private DeployedResourcesScanner<StackGresScript> deployedResourcesScanner;

  @BeforeEach
  void setUp() {
  }

  @Override
  protected Conciliator<StackGresScript> buildConciliator(List<HasMetadata> required,
      List<HasMetadata> deployed) {
    when(requiredResourceGenerator.getRequiredResources(script))
        .thenReturn(required);
    when(deployedResourcesScanner.getDeployedResources(script))
        .thenReturn(deployed);

    final ScriptConciliator scriptConciliator = new ScriptConciliator();
    scriptConciliator.setRequiredResourceGenerator(requiredResourceGenerator);
    scriptConciliator.setDeployedResourcesScanner(deployedResourcesScanner);
    scriptConciliator.setResourceComparator(resourceComparator);
    return scriptConciliator;
  }

  @Override
  protected StackGresScript getConciliationResource() {
    return script;
  }

}
