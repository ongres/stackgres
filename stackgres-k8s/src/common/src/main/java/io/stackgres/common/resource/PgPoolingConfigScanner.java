/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigList;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PgPoolingConfigScanner
    extends AbstractCustomResourceScanner<StackGresPoolingConfig, StackGresPoolingConfigList> {

  @Inject
  public PgPoolingConfigScanner(KubernetesClient client) {
    super(client, StackGresPoolingConfig.class, StackGresPoolingConfigList.class);
  }

}

