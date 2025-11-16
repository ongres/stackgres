/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.event;

import java.util.Map;

import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.labels.LabelFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DbOpsEventEmitter extends AbstractEventEmitter<StackGresDbOps> {

  private final LabelFactory<StackGresDbOps> labelFactory;

  @Inject
  public DbOpsEventEmitter(LabelFactory<StackGresDbOps> labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  protected Map<String, String> getLabels(StackGresDbOps involvedObject) {
    return labelFactory.genericLabels(involvedObject);
  }

}
