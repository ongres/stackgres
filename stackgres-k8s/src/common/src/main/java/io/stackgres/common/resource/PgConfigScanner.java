/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.CdiUtil;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigList;

@ApplicationScoped
public class PgConfigScanner extends
    AbstractCustomResourceScanner<StackGresPostgresConfig, StackGresPostgresConfigList> {

  @Inject
  public PgConfigScanner(KubernetesClientFactory clientFactory) {
    super(clientFactory,         StackGresPostgresConfig.class, StackGresPostgresConfigList.class);
  }

  public PgConfigScanner() {
    super(null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

}
