/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.crd.sgconfig.StackGresConfig;

@ApplicationScoped
public class ConfigLabelFactory extends AbstractLabelFactory<StackGresConfig>
    implements LabelFactoryForConfig {

  private final LabelMapperForConfig labelMapper;

  @Inject
  public ConfigLabelFactory(LabelMapperForConfig labelMapper) {
    this.labelMapper = labelMapper;
  }

  @Override
  public LabelMapperForConfig labelMapper() {
    return labelMapper;
  }

}
