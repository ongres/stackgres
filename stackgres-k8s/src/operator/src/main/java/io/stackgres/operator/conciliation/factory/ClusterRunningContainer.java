/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

public enum ClusterRunningContainer {
  NONE,
  PATRONI_V09,
  POSTGRES_UTIL_V09,
  ENVOY_V09,
  FLUENT_BIT_V09,
  FLUENTD_V09,
  POSTGRES_EXPORTER_V09,
  PGBOUNCER_V09,
  PATRONI,
  ENVOY,
  POSTGRES_EXPORTER,
  FLUENT_BIT,
  FLUENTD,
  CLUSTER_CONTROLLER,
  PGBOUNCER,
  POSTGRES_UTIL,
  ;
}
