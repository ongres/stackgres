/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigList;

@ApplicationScoped
public class PgPoolingConfigScheduler
    extends AbstractCustomResourceScheduler<StackGresPoolingConfig, StackGresPoolingConfigList> {

  @Inject
  public PgPoolingConfigScheduler(KubernetesClient client) {
    super(client, StackGresPoolingConfig.class, StackGresPoolingConfigList.class);
  }

  public PgPoolingConfigScheduler() {
    super(null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

}
