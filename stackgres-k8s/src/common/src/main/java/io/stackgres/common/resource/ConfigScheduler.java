/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigList;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ConfigScheduler extends
    AbstractCustomResourceScheduler<StackGresConfig, StackGresConfigList> {

  public ConfigScheduler() {
    super(StackGresConfig.class, StackGresConfigList.class);
  }

  @Override
  public StackGresConfig update(StackGresConfig resource) {
    if (resource.getMetadata().getResourceVersion() == null) {
      return super.update(resource);
    }
    return client.resources(StackGresConfig.class, StackGresConfigList.class)
        .resource(resource)
        .lockResourceVersion(resource.getMetadata().getResourceVersion())
        .replace();
  }

}
