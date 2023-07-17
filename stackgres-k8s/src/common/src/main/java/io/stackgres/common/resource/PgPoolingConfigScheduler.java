/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigList;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PgPoolingConfigScheduler
    extends AbstractCustomResourceScheduler<StackGresPoolingConfig, StackGresPoolingConfigList> {

  public PgPoolingConfigScheduler() {
    super(StackGresPoolingConfig.class, StackGresPoolingConfigList.class);
  }

}
