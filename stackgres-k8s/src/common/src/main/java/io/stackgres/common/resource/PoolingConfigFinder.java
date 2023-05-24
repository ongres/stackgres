/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigList;

@ApplicationScoped
public class PoolingConfigFinder
    extends AbstractCustomResourceFinder<StackGresPoolingConfig> {

  public PoolingConfigFinder(KubernetesClient client) {
    super(client, StackGresPoolingConfig.class, StackGresPoolingConfigList.class);
  }

}
