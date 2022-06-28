/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;

public enum StackGresKind {

  CLUSTER(StackGresCluster.class),
  DBOPS(StackGresDbOps.class, "dbops"),
  BACKUP(StackGresBackup.class, "backup");

  private final Class<? extends CustomResource<?, ?>> kind;
  private final String containerPrefix;

  StackGresKind(Class<? extends CustomResource<?, ?>> kind,
      String containerPrefix) {
    this.kind = kind;
    this.containerPrefix = containerPrefix + ".";
  }

  StackGresKind(Class<? extends CustomResource<?, ?>> kind) {
    this.kind = kind;
    this.containerPrefix = "";
  }

  public Class<? extends CustomResource<?, ?>> getKindType() {
    return kind;
  }

  public String getContainerPrefix() {
    return containerPrefix;
  }

  public boolean hasPrefix(String name) {
    if (containerPrefix.isEmpty()) {
      return !name.contains(".");
    }
    return name.startsWith(containerPrefix);
  }

  public String getName(String name) {
    return name.substring(containerPrefix.length());
  }

  @Override
  public String toString() {
    return getKindType().toString();
  }

}
