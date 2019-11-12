/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgbackupconfig;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class StackGresBackupConfigDoneable
    extends CustomResourceDoneable<StackGresBackupConfig> {

  public StackGresBackupConfigDoneable(StackGresBackupConfig resource,
      Function<StackGresBackupConfig, StackGresBackupConfig> function) {
    super(resource, function);
  }

}
