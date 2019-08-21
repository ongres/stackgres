/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresources.sgprofile;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class StackGresProfileDoneable
    extends CustomResourceDoneable<StackGresProfile> {

  public StackGresProfileDoneable(StackGresProfile resource,
      Function<StackGresProfile, StackGresProfile> function) {
    super(resource, function);
  }

}
