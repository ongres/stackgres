/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgscript.StackGresScript;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ScriptLabelMapper implements LabelMapperForScript {

  @Override
  public String appName() {
    return StackGresContext.SCRIPT_APP_NAME;
  }

  @Override
  public String resourceNameKey(StackGresScript resource) {
    return getKeyPrefix(resource) + StackGresContext.SCRIPT_NAME_KEY;
  }

  @Override
  public String resourceNamespaceKey(StackGresScript resource) {
    return getKeyPrefix(resource) + StackGresContext.SCRIPT_NAMESPACE_KEY;
  }

  @Override
  public String resourceUidKey(StackGresScript resource) {
    return getKeyPrefix(resource) + StackGresContext.SCRIPT_UID_KEY;
  }

}
