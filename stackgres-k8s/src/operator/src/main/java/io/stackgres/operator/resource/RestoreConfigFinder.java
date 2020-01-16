/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.customresource.sgrestoreconfig.StackgresRestoreConfig;
import io.stackgres.operator.customresource.sgrestoreconfig.StackgresRestoreConfigDefinition;
import io.stackgres.operator.customresource.sgrestoreconfig.StackgresRestoreConfigDoneable;
import io.stackgres.operator.customresource.sgrestoreconfig.StackgresRestoreConfigList;

@ApplicationScoped
public class RestoreConfigFinder
    extends AbstractKubernetesCustomResourceFinder<StackgresRestoreConfig> {

  @Inject
  protected RestoreConfigFinder(
      KubernetesClientFactory clientFactory) {
    super(clientFactory,
        StackgresRestoreConfigDefinition.NAME,
        StackgresRestoreConfig.class,
        StackgresRestoreConfigList.class,
        StackgresRestoreConfigDoneable.class);
  }

  public RestoreConfigFinder() {

    super(null, null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}
