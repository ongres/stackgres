/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import io.stackgres.common.crd.sgscript.StackGresScript;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ScriptLabelFactory
    extends AbstractLabelFactory<StackGresScript> implements LabelFactoryForScript {

  private final LabelMapperForScript labelMapper;

  @Inject
  public ScriptLabelFactory(LabelMapperForScript labelMapper) {
    this.labelMapper = labelMapper;
  }

  @Override
  public LabelMapperForScript labelMapper() {
    return labelMapper;
  }

}
