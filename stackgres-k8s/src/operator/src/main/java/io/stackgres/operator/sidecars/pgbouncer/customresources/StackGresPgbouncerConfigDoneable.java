/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.sidecars.pgbouncer.customresources;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class StackGresPgbouncerConfigDoneable
    extends CustomResourceDoneable<StackGresPgbouncerConfig> {

  public StackGresPgbouncerConfigDoneable(StackGresPgbouncerConfig resource,
      Function<StackGresPgbouncerConfig, StackGresPgbouncerConfig> function) {
    super(resource, function);
  }

}
