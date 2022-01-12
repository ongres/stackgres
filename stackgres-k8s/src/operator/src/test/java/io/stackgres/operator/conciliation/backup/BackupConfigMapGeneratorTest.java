/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import java.util.Map;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.StackGresVersion;
import io.stackgres.operator.conciliation.GeneratorTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class BackupConfigMapGeneratorTest extends GeneratorTest {

  @Test
  @DisplayName("Test backup ConfigMap for 0.9.5 without backup configuration")
  void testBackupConfigMapForV095() {

    var expectedConfigMap = new ConfigMapBuilder()
        .withNewMetadata()
        .withName(CLUSTER_NAME + "-backup")
        .withNamespace(CLUSTER_NAMESPACE)
        .withAnnotations(Map.of("allResourceAnnotation", "allResourceValue"))
        .withLabels(Map.of(
            "cluster-uid", CLUSTER_UID,
            "cluster-name", CLUSTER_NAME,
            "cluster", "true",
            "app", "StackGresCluster"
        ))
        .endMetadata()
        .withData(Map.of(
            "MD5SUM", "D41D8CD98F00B204E9800998ECF8427E"
        ))
        .build();

    givenAClusterWithVersion(StackGresVersion.V09_LAST)
        .andAllResourceAnnotations(Map.of("allResourceAnnotation", "allResourceValue"))
        .generatedResourceShouldBeEqualTo(expectedConfigMap);
  }
}
