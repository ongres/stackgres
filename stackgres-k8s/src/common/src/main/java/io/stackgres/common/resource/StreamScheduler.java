/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamList;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StreamScheduler
    extends AbstractCustomResourceScheduler<StackGresStream, StackGresStreamList> {

  public StreamScheduler() {
    super(StackGresStream.class, StackGresStreamList.class);
  }

  @Override
  public StackGresStream update(StackGresStream resource) {
    if (resource.getMetadata().getResourceVersion() == null) {
      return super.update(resource);
    }
    return client.resources(StackGresStream.class, StackGresStreamList.class)
        .resource(resource)
        .lockResourceVersion(resource.getMetadata().getResourceVersion())
        .update();
  }

}
