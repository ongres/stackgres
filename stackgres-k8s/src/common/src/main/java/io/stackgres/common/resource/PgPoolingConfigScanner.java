/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.CdiUtil;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigList;

@ApplicationScoped
public class PgPoolingConfigScanner
    extends AbstractCustomResourceScanner<StackGresPoolingConfig, StackGresPoolingConfigList> {

  @Inject
  public PgPoolingConfigScanner(KubernetesClientFactory clientFactory) {
    super(clientFactory, StackGresPoolingConfig.class, StackGresPoolingConfigList.class);
  }

  public PgPoolingConfigScanner() {
    super(null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

}

