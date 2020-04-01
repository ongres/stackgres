/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.sidecars.pooling.customresources.StackGresPoolingConfig;
import io.stackgres.operator.sidecars.pooling.customresources.StackGresPoolingConfigDefinition;
import io.stackgres.operator.sidecars.pooling.customresources.StackGresPoolingConfigDoneable;
import io.stackgres.operator.sidecars.pooling.customresources.StackGresPoolingConfigList;

@ApplicationScoped
public class PoolingConfigFinder
    extends AbstractCustomResourceFinder<StackGresPoolingConfig> {

  /**
   * Create a {@code PoolingConfigFinder} instance.
   */
  @Inject
  public PoolingConfigFinder(KubernetesClientFactory clientFactory) {
    super(clientFactory, StackGresPoolingConfigDefinition.NAME,
        StackGresPoolingConfig.class, StackGresPoolingConfigList.class,
        StackGresPoolingConfigDoneable.class);
  }

  public PoolingConfigFinder() {
    super(null, null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}
