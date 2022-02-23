/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.script;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ScriptRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresScript> {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(ScriptRequiredResourcesGenerator.class);

  @Override
  public List<HasMetadata> getRequiredResources(StackGresScript config) {
    return List.of();
  }

}
