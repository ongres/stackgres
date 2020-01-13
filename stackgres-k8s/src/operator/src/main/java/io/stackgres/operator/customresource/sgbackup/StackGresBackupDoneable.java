/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgbackup;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class StackGresBackupDoneable
    extends CustomResourceDoneable<StackGresBackup> {

  public StackGresBackupDoneable(StackGresBackup resource,
      Function<StackGresBackup, StackGresBackup> function) {
    super(resource, function);
  }

}
