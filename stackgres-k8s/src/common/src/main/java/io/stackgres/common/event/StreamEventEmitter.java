/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.event;

import java.util.Map;

import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.labels.LabelFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StreamEventEmitter extends AbstractEventEmitter<StackGresStream> {

  private final LabelFactory<StackGresStream> labelFactory;

  @Inject
  public StreamEventEmitter(LabelFactory<StackGresStream> labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  protected Map<String, String> getLabels(StackGresStream involvedObject) {
    return labelFactory.genericLabels(involvedObject);
  }

}
