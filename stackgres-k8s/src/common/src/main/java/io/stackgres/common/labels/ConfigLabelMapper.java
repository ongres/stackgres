/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ConfigLabelMapper implements LabelMapperForConfig {

  @Override
  public String appName() {
    return StackGresContext.CONFIG_APP_NAME;
  }

  @Override
  public String resourceNameKey(StackGresConfig resource) {
    return getKeyPrefix(resource) + StackGresContext.CONFIG_NAME_KEY;
  }

  @Override
  public String resourceNamespaceKey(StackGresConfig resource) {
    return getKeyPrefix(resource) + StackGresContext.CONFIG_NAMESPACE_KEY;
  }

  @Override
  public String resourceUidKey(StackGresConfig resource) {
    return getKeyPrefix(resource) + StackGresContext.CONFIG_UID_KEY;
  }

}
