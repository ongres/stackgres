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
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigDefinition;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigDoneable;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigList;

@ApplicationScoped
public class PgConfigScheduler
    extends AbstractCustomResourceScheduler<StackGresPostgresConfig,
    StackGresPostgresConfigList, StackGresPostgresConfigDoneable> {

  @Inject
  public PgConfigScheduler(KubernetesClientFactory clientFactory) {
    super(clientFactory,
        StackGresPostgresConfigDefinition.CONTEXT,
        StackGresPostgresConfig.class,
        StackGresPostgresConfigList.class,
        StackGresPostgresConfigDoneable.class);
  }

  public PgConfigScheduler() {
    super(null, null, null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

}
