/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigList;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PgConfigScanner extends
    AbstractCustomResourceScanner<StackGresPostgresConfig, StackGresPostgresConfigList> {

  @Inject
  public PgConfigScanner(KubernetesClient client) {
    super(client,         StackGresPostgresConfig.class, StackGresPostgresConfigList.class);
  }

}
