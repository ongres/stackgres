/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class StackGresDbOpsDoneable
    extends CustomResourceDoneable<StackGresDbOps> {

  public StackGresDbOpsDoneable(StackGresDbOps resource,
      Function<StackGresDbOps, StackGresDbOps> function) {
    super(resource, function);
  }

}
