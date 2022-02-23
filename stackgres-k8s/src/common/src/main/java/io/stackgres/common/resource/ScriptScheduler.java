/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptList;

@ApplicationScoped
public class ScriptScheduler extends
    AbstractCustomResourceScheduler<StackGresScript, StackGresScriptList> {

  public ScriptScheduler() {
    super(StackGresScript.class, StackGresScriptList.class);
  }

  @Override
  public StackGresScript update(StackGresScript resource) {
    return client.resources(StackGresScript.class, StackGresScriptList.class)
        .inNamespace(resource.getMetadata().getNamespace())
        .withName(resource.getMetadata().getName())
        .lockResourceVersion(resource.getMetadata().getResourceVersion())
        .replace(resource);
  }

}
