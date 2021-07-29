/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.crd.sgdbops.StackGresDbOps;

@ApplicationScoped
public class DbOpsLabelFactory extends AbstractLabelFactoryForDbOps<StackGresDbOps> {

  private final LabelMapperForDbOps<StackGresDbOps> labelMapper;

  @Inject
  public DbOpsLabelFactory(LabelMapperForDbOps<StackGresDbOps> labelMapper) {
    this.labelMapper = labelMapper;
  }

  @Override
  public LabelMapperForDbOps<StackGresDbOps> labelMapper() {
    return labelMapper;
  }

}
