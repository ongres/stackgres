/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.customresource.sgpgbouncer;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class StackGresPgBouncerDoneable extends CustomResourceDoneable<StackGresPgBouncerConfig> {

  public StackGresPgBouncerDoneable(StackGresPgBouncerConfig resource,
                                    Function<StackGresPgBouncerConfig,
                                    StackGresPgBouncerConfig> function) {
    super(resource, function);
  }
}
