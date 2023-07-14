/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptList;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ScriptScheduler extends
    AbstractCustomResourceScheduler<StackGresScript, StackGresScriptList> {

  public ScriptScheduler() {
    super(StackGresScript.class, StackGresScriptList.class);
  }

  @Override
  public StackGresScript update(StackGresScript resource) {
    if (resource.getMetadata().getResourceVersion() == null) {
      return super.update(resource);
    }
    return client.resources(StackGresScript.class, StackGresScriptList.class)
        .resource(resource)
        .lockResourceVersion(resource.getMetadata().getResourceVersion())
        .update();
  }

}
