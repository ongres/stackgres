/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

public interface StackgresClusterContainers {

  String PATRONI = "patroni";

  String ENVOY = "envoy";

  String FLUENT_BIT = "fluent-bit";

  String POSTGRES_EXPORTER = "prometheus-postgres-exporter";

  String POSTGRES_UTIL = "postgres-util";

  String FLUENTD = "fluentd";

  String CLUSTER_CONTROLLER = "cluster-controller";

  String DISTRIBUTEDLOGS_CONTROLLER = "distributedlogs-controller";

  String MAJOR_VERSION_UPGRADE = "major-version-upgrade";
}
