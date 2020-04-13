/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigDefinition;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigDoneable;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigList;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.common.ArcUtil;

@ApplicationScoped
public class PgConfigScanner extends
    AbstractCustomResourceScanner<StackGresPostgresConfig, StackGresPostgresConfigList,
    StackGresPostgresConfigDoneable> {

  @Inject
  public PgConfigScanner(KubernetesClientFactory clientFactory) {
    super(clientFactory, StackGresPostgresConfigDefinition.NAME,
        StackGresPostgresConfig.class, StackGresPostgresConfigList.class,
        StackGresPostgresConfigDoneable.class);
  }

  public PgConfigScanner() {
    super(null, null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}
