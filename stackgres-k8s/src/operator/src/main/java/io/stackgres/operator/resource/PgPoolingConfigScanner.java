/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.cluster.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.operator.cluster.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigDefinition;
import io.stackgres.operator.cluster.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigDoneable;
import io.stackgres.operator.cluster.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigList;
import io.stackgres.operator.common.ArcUtil;

@ApplicationScoped
public class PgPoolingConfigScanner extends AbstractKubernetesCustomResourceScanner
    <StackGresPgbouncerConfig, StackGresPgbouncerConfigList, StackGresPgbouncerConfigDoneable> {

  @Inject
  public PgPoolingConfigScanner(KubernetesClientFactory clientFactory) {
    super(clientFactory, StackGresPgbouncerConfigDefinition.NAME,
        StackGresPgbouncerConfig.class, StackGresPgbouncerConfigList.class,
        StackGresPgbouncerConfigDoneable.class);
  }

  public PgPoolingConfigScanner() {
    super(null, null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}

