/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgpgconfig;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class StackGresPostgresConfigDoneable
    extends CustomResourceDoneable<StackGresPostgresConfig> {

  public StackGresPostgresConfigDoneable(StackGresPostgresConfig resource,
      Function<StackGresPostgresConfig, StackGresPostgresConfig> function) {
    super(resource, function);
  }

}
