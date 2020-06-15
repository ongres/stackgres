/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdistributedlogs;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class StackGresDistributedLogsDoneable
    extends CustomResourceDoneable<StackGresDistributedLogs> {

  public StackGresDistributedLogsDoneable(StackGresDistributedLogs resource,
      Function<StackGresDistributedLogs, StackGresDistributedLogs> function) {
    super(resource, function);
  }

}
