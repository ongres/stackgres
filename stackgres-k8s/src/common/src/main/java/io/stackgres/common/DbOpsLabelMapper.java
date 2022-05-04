/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgdbops.StackGresDbOps;

@ApplicationScoped
public class DbOpsLabelMapper implements LabelMapperForDbOps {

  @Override
  public String appName() {
    return StackGresContext.DBOPS_APP_NAME;
  }

  @Override
  public String resourceNameKey(StackGresDbOps resource) {
    return getKeyPrefix(resource) + StackGresContext.DBOPS_NAME_KEY;
  }

  @Override
  public String resourceNamespaceKey(StackGresDbOps resource) {
    return getKeyPrefix(resource) + StackGresContext.DBOPS_NAMESPACE_KEY;
  }

  @Override
  public String resourceUidKey(StackGresDbOps resource) {
    return getKeyPrefix(resource) + StackGresContext.DBOPS_UID_KEY;
  }

}
