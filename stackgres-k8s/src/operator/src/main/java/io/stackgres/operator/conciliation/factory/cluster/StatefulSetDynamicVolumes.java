/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import io.stackgres.operatorframework.resource.ResourceUtil;

public enum StatefulSetDynamicVolumes {

  PATRONI_ENV("patroni-env", "%s"),
  SCRIPT_TEMPLATES("templates", "%s-templates"),
  BACKUP_CREDENTIALS("backup-secret", "%s-backup"),
  BACKUP_ENV("backup-env", "%s-backup"),
  RESTORE_CREDENTIALS("restore-secret", "%s-restore"),
  RESTORE_ENV("restore-env", "%s-restore"),
  ENVOY("envoy", "%s-envoy-config"),
  EXPORTER_QUERIES("queries", "%s-prometheus-postgres-exporter-config"),
  EXPORTER_INIT("postgres-exporter-init", "%s-prometheus-postgresx"),
  PG_BOUNCER("pgbouncer", "%s-connection-pooling-config"),
  FLUENT_BIT("fluent-bit", "%s-fluent-bit");

  private final String volumeName;
  private final String resourceNameFormat;

  StatefulSetDynamicVolumes(String volumeName, String resourceFormat) {
    this.volumeName = volumeName;
    this.resourceNameFormat = resourceFormat;
  }

  public String getVolumeName() {
    return volumeName;
  }

  public String getResourceName(String clusterName) {
    return ResourceUtil.resourceName(String.format(resourceNameFormat, clusterName));
  }
}
