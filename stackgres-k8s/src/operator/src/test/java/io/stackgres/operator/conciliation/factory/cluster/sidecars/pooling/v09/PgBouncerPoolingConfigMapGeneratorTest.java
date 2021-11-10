/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.v09;

import java.util.Map;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.operator.common.StackGresVersion;
import io.stackgres.operator.conciliation.GeneratorTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PgBouncerPoolingConfigMapGeneratorTest extends GeneratorTest {

  @Test
  @DisplayName("Test pooling config for version 0.9.5")
  void testPoolingConfigForV095() {

    var expectedConfigMap = new ConfigMapBuilder()
        .withNewMetadata()
        .withName(CLUSTER_NAME + "-connection-pooling-config")
        .withNamespace(CLUSTER_NAMESPACE)
        .withAnnotations(Map.of("allResourceAnnotation", "allResourceValue"))
        .withLabels(Map.of(
            "cluster-uid", CLUSTER_UID,
            "cluster-name", CLUSTER_NAME,
            "app", "StackGresCluster"
        ))
        .endMetadata()
        .withData(
            Map.of("pgbouncer.ini", "|\n"
                + "[databases]\n"
                + " * = port = 5432\n"
                + "\n"
                + "[pgbouncer]\n"
                + "listen_port = 6432\n"
                + "unix_socket_dir = /var/run/postgresql\n"
                + "stats_users = postgres\n"
                + "ignore_startup_parameters = extra_float_digits\n"
                + "auth_type = md5\n"
                + "max_db_connections = 0\n"
                + "pool_mode = session\n"
                + "auth_query = SELECT usename, passwd FROM pg_shadow WHERE usename=$1\n"
                + "application_name_add_host = 1\n"
                + "max_client_conn = 1000\n"
                + "default_pool_size = 1000\n"
                + "max_user_connections = 0\n"
                + "auth_user = authenticator\n"
                + "listen_addr = 127.0.0.1\n"
                + "admin_users = postgres\n")
        )
        .build();

    givenAClusterWithVersion(StackGresVersion.V09_LAST)
        .andAllResourceAnnotations(Map.of("allResourceAnnotation", "allResourceValue"))
        .generatedResourceShouldBeEqualTo(expectedConfigMap);
  }

}
