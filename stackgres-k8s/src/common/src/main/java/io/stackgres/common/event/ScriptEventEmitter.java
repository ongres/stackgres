/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.event;

import java.util.Map;

import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.labels.LabelFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ScriptEventEmitter extends AbstractEventEmitter<StackGresScript> {

  private final LabelFactory<StackGresScript> labelFactory;

  @Inject
  public ScriptEventEmitter(LabelFactory<StackGresScript> labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  protected Map<String, String> getLabels(StackGresScript involvedObject) {
    return labelFactory.genericLabels(involvedObject);
  }

}
