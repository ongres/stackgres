/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pgexporter.v09;

import java.util.Map;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.operator.common.StackGresVersion;
import io.stackgres.operator.conciliation.GeneratorTest;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PostgresExporterGeneratorTest extends GeneratorTest {

  @Test
  @DisplayName("Test postgres exporter init ConfigMap generation for 0.9.5 ")
  void testPostgresExporterInitConfigMapV095() {

    var expectedConfigMap = new ConfigMapBuilder()
        .withNewMetadata()
        .withName(ResourceUtil.cutVolumeName(CLUSTER_NAME
            + "-internal-00000-prometheus-postgres-exporter-init-postgres"))
        .withNamespace(CLUSTER_NAMESPACE)
        .withAnnotations(Map.of("allResourceAnnotation", "allResourceValue"))
        .withLabels(Map.of(
            "cluster-uid", CLUSTER_UID,
            "cluster-name", CLUSTER_NAME,
            "app", "StackGresCluster",
            "cluster", "true"
        ))
        .endMetadata()
        .withData(Map.of("00000-prometheus-postgres-exporter-init.postgres.sql",
            "CREATE EXTENSION IF NOT EXISTS dblink;\n"
                + "CREATE EXTENSION IF NOT EXISTS plpython3u;\n"
                + "\n"
                + "CREATE OR REPLACE FUNCTION df(path text)\n"
                + "RETURNS SETOF text\n"
                + "AS\n"
                + "$$\n"
                + "  import subprocess\n"
                + "  try:\n"
                + "    result = subprocess.run(['df', '-B1', "
                + "'--output=source,target,fstype,size,avail,itotal,iavail', path], timeout=1, "
                + "stdout=subprocess.PIPE, stderr=subprocess.PIPE, encoding='UTF-8')\n"
                + "  except:\n"
                + "    return ['- ' + path + ' - - - - - timeout']\n"
                + "  if result.returncode == 0:\n"
                + "    return result.stdout.split('\\n')[1:2]\n"
                + "  else:\n"
                + "    return ['- ' + path + ' - - - - - ' + result.stderr.replace(' ', '\\\\s')]\n"
                + "$$\n"
                + "LANGUAGE plpython3u;\n"
                + "\n"
                + "CREATE OR REPLACE FUNCTION mounts()\n"
                + "RETURNS SETOF text\n"
                + "AS\n"
                + "$$\n"
                + "  import subprocess\n"
                + "  return subprocess.run(['cat', '/proc/mounts'], "
                + "stdout=subprocess.PIPE, encoding='UTF-8').stdout.split('\\n')\n"
                + "$$\n"
                + "LANGUAGE plpython3u;\n"))
        .build();

    givenAClusterWithVersion(StackGresVersion.V09_LAST)
        .andAllResourceAnnotations(Map.of("allResourceAnnotation", "allResourceValue"))
        .generatedResourceShouldBeEqualTo(expectedConfigMap);

  }

}
