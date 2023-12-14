/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

public enum StackGresVolume implements StackGresNamedObject {

  PATRONI_ENV("patroni-env", "%s"),
  PATRONI_CREDENTIALS("patroni-secret-env", "%s"),
  SCRIPT_TEMPLATES("templates", "%s-templates"),
  BACKUP_CREDENTIALS("backup-secret", "%s-backup"),
  BACKUP_ENV("backup-env", "%s-backup"),
  RESTORE_CREDENTIALS("restore-secret", "%s-restore"),
  RESTORE_ENV("restore-env", "%s-restore"),
  REPLICATE_CREDENTIALS("replicate-secret", "%s-replicate"),
  REPLICATE_ENV("replicate-env", "%s-replicate"),
  ENVOY("envoy", "%s-envoy-config"),
  EXPORTER_QUERIES("queries", "%s-prometheus-postgres-exporter-config"),
  EXPORTER_INIT("postgres-exporter-init", "%s-prometheus-postgres"),
  PGBOUNCER_CONFIG("pgbouncer", "%s-connection-pooling-config"),
  PGBOUNCER_DYNAMIC_CONFIG("pgbouncer-dynamic-config"),
  PGBOUNCER_SECRETS("pgbouncer-secrets"),
  FLUENT_BIT("fluent-bit", "%s-fluent-bit"),
  POSTGRES_CONFIG("postgresql-conf", "%s-postgresql-conf"),
  POSTGRES_SOCKET("socket"),
  POSTGRES_SSL("ssl"),
  POSTGRES_SSL_COPY("ssl-copy"),
  INIT_SCRIPT("distributed-logs-template", "%s-init-template"),
  FLUENTD_CONFIG("fluentd-config", "%s-fluentd"),
  DSHM("dshm"),
  SHARED("shared"),
  EMPTY_BASE("empty-base"),
  USER("user"),
  LOCAL_BIN("local-bin"),
  LOG("log"),
  PATRONI_CONFIG("patroni-config"),
  HUGEPAGES_2M("hugepages-2mi"),
  HUGEPAGES_1G("hugepages-1gi"),
  FLUENTD("fluentd"),
  FLUENTD_BUFFER("fluentd-buffer"),
  FLUENTD_LOG("fluentd-log"),
  CUSTOM("custom-%s");

  private final String name;
  private final String resourceNameFormat;

  StackGresVolume(String name) {
    this.name = name;
    this.resourceNameFormat = null;
  }

  StackGresVolume(String name,
      String resourceNameFormat) {
    this.name = name;
    this.resourceNameFormat = resourceNameFormat;
  }

  @Override
  public String getName() {
    return name;
  }

  public String getResourceNameFormat() {
    return resourceNameFormat;
  }

  public String getResourceName(String...parameters) {
    return format(getResourceNameFormat(), parameters);
  }

  @Override
  public String toString() {
    return getName();
  }

}
