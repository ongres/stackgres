/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class DbOpsLabelFactory extends AbstractLabelFactoryForDbOps {

  private final LabelMapperForDbOps labelMapper;

  @Inject
  public DbOpsLabelFactory(LabelMapperForDbOps labelMapper) {
    this.labelMapper = labelMapper;
  }

  @Override
  public LabelMapperForDbOps labelMapper() {
    return labelMapper;
  }

}
