/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.Map;

import io.fabric8.kubernetes.api.model.Service;
import io.stackgres.common.event.AbstractEventEmitter;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OperatorEventEmitter extends AbstractEventEmitter<Service> {

  @Override
  protected Map<String, String> getLabels(Service involvedObject) {
    return involvedObject.getMetadata().getLabels();
  }

}
