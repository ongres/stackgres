/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.event;

import java.util.Map;

import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.labels.LabelFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ConfigEventEmitter extends AbstractEventEmitter<StackGresConfig> {

  private final LabelFactory<StackGresConfig> labelFactory;

  @Inject
  public ConfigEventEmitter(LabelFactory<StackGresConfig> labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  protected Map<String, String> getLabels(StackGresConfig involvedObject) {
    return labelFactory.genericLabels(involvedObject);
  }

}
