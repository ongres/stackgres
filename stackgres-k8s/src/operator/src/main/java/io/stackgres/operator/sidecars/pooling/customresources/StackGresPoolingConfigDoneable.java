/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.sidecars.pooling.customresources;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class StackGresPoolingConfigDoneable
    extends CustomResourceDoneable<StackGresPoolingConfig> {

  public StackGresPoolingConfigDoneable(
      StackGresPoolingConfig resource,
      Function<StackGresPoolingConfig, StackGresPoolingConfig> function) {
    super(resource, function);
  }

}
