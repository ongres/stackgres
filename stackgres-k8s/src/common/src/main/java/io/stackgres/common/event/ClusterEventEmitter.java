/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.event;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operatorframework.resource.EventReason;

@ApplicationScoped
public class ClusterEventEmitter extends AbstractEventEmitter<StackGresCluster> {

  @Override
  public void sendEvent(EventReason reason, String message, StackGresCluster context) {

    emitEvent(reason, message, context);
  }
}
