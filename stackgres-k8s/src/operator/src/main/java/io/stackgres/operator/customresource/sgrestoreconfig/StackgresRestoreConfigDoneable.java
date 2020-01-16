/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgrestoreconfig;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class StackgresRestoreConfigDoneable
    extends CustomResourceDoneable<StackgresRestoreConfig> {

  public StackgresRestoreConfigDoneable(
      StackgresRestoreConfig resource,
      Function<StackgresRestoreConfig, StackgresRestoreConfig> function) {
    super(resource, function);
  }
}
